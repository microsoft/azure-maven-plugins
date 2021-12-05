/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.task;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeployFunctionAppTask extends AzureTask<IFunctionAppBase<?>> {

    private static final int SYNC_FUNCTION_MAX_ATTEMPTS = 5;
    private static final int SYNC_FUNCTION_DELAY = 1;
    private static final int LIST_TRIGGERS_MAX_RETRY = 5;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;
    private static final String RUNNING = "Running";
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";
    private static final String DEPLOY_START = "Starting deployment...";
    private static final String DEPLOY_FINISH = "Deployment done, you may access your resource through %s";
    private static final String HTTP_TRIGGER_URLS = "HTTP Trigger Urls:";
    private static final String NO_ANONYMOUS_HTTP_TRIGGER = "No anonymous HTTP Triggers found in deployed function app, skip list triggers.";
    private static final String NO_TRIGGERS_FOUNDED = "No triggers found in deployed function app, " +
            "please try recompile the project by `mvn clean package` and deploy again.";
    private static final String UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS = "Some http trigger urls cannot be displayed " +
            "because they are non-anonymous. To access the non-anonymous triggers, please refer to https://aka.ms/azure-functions-key.";
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker app service";
    private static final String FAILED_TO_LIST_TRIGGERS = "Deployment succeeded, but failed to list http trigger urls.";
    private static final String SYNC_TRIGGERS = "Syncing triggers and fetching function information";
    private static final String LIST_TRIGGERS = "Querying triggers...";
    private static final String LIST_TRIGGERS_WITH_RETRY = "Querying triggers (Attempt {0}/{1})...";

    private final IFunctionAppBase<?> target;
    private final File stagingDirectory;
    private final FunctionDeployType deployType;

    public DeployFunctionAppTask(@Nonnull IFunctionAppBase<?> target, @Nonnull File stagingFolder, @Nullable FunctionDeployType deployType) {
        this.target = target;
        this.stagingDirectory = stagingFolder;
        this.deployType = deployType;
    }

    @Override
    public AzureString getTitle() {
        return AzureString.format("Deploy artifact to function app %s", target.name());
    }

    @Override
    public IFunctionAppBase<?> execute() {
        if (target.getRuntime().isDocker()) {
            AzureMessager.getMessager().info(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE);
            return target;
        }
        deployArtifact();
        if (target instanceof FunctionApp) {
            listHTTPTriggerUrls((FunctionApp) target);
        }
        return target;
    }

    private void deployArtifact() {
        AzureMessager.getMessager().info(DEPLOY_START);
        // For ftp deploy, we need to upload entire staging directory not the zipped package
        final File file = deployType == FunctionDeployType.FTP ? stagingDirectory : packageStagingDirectory();
        final long startTime = System.currentTimeMillis();
        if (deployType == null) {
            target.deploy(file);
        } else {
            target.deploy(file, deployType);
        }
        AzureTelemetry.getActionContext().setProperty("deploy-cost", String.valueOf(System.currentTimeMillis() - startTime));
        if (!StringUtils.equalsIgnoreCase(target.state(), RUNNING)) {
            target.start();
        }
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, target.hostName()));
    }

    private File packageStagingDirectory() {
        try {
            final File zipFile = Files.createTempFile("azure-functions", ".zip").toFile();
            ZipUtil.pack(stagingDirectory, zipFile);
            ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
            return zipFile;
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException("Failed to package function to deploy", e);
        }
    }

    private void listHTTPTriggerUrls(FunctionApp target) {
        try {
            syncTriggers(target);
            final List<FunctionEntity> triggers = listFunctions(target);
            final List<FunctionEntity> httpFunction = triggers.stream()
                    .filter(function -> function.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(function.getTrigger().getType(), HTTP_TRIGGER))
                    .collect(Collectors.toList());
            final List<FunctionEntity> anonymousTriggers = httpFunction.stream()
                    .filter(bindingResource -> bindingResource.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(bindingResource.getTrigger().getProperty(AUTH_LEVEL), AuthorizationLevel.ANONYMOUS.toString()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
                AzureMessager.getMessager().info(NO_ANONYMOUS_HTTP_TRIGGER);
                return;
            }
            AzureMessager.getMessager().info(HTTP_TRIGGER_URLS);
            anonymousTriggers.forEach(trigger -> AzureMessager.getMessager().info(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())));
            if (anonymousTriggers.size() < httpFunction.size()) {
                AzureMessager.getMessager().info(UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS);
            }
        } catch (final RuntimeException | InterruptedException e) {
            // show warning instead of exception for list triggers
            AzureMessager.getMessager().warning(FAILED_TO_LIST_TRIGGERS);
        }
    }

    // todo: move to app service library
    // Refers https://github.com/Azure/azure-functions-core-tools/blob/3.0.3568/src/Azure.Functions.Cli/Actions/AzureActions/PublishFunctionAppAction.cs#L452
    private void syncTriggers(final FunctionApp functionApp) throws InterruptedException {
        AzureMessager.getMessager().info(SYNC_TRIGGERS);
        Thread.sleep(5 * 1000);
        Mono.fromRunnable(() -> {
            try {
                Azure.az(AzureAppService.class).getAppServiceManager(functionApp.subscriptionId())
                        .functionApps().manager().serviceClient().getWebApps().syncFunctions(functionApp.resourceGroup(), functionApp.name());
            } catch (ManagementException e) {
                if (e.getResponse().getStatusCode() == 200) {
                    // Java SDK throw exception with 200 response, swallow exception in this case
                }
            }
        }).subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.fixedDelay(SYNC_FUNCTION_MAX_ATTEMPTS - 1, Duration.ofSeconds(SYNC_FUNCTION_DELAY))).block();
    }

    private List<FunctionEntity> listFunctions(final FunctionApp functionApp) {
        final int[] count = {0};
        return Mono.fromCallable(() -> {
            final AzureString message = count[0]++ == 0 ?
                    AzureString.fromString(LIST_TRIGGERS) : AzureString.format(LIST_TRIGGERS_WITH_RETRY, count[0], LIST_TRIGGERS_MAX_RETRY);
            AzureMessager.getMessager().info(message);
            return Optional.ofNullable(functionApp.listFunctions(true))
                    .filter(CollectionUtils::isNotEmpty)
                    .orElseThrow(() -> new AzureToolkitRuntimeException(NO_TRIGGERS_FOUNDED));
        }).subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.fixedDelay(LIST_TRIGGERS_MAX_RETRY - 1, Duration.ofSeconds(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS))).block();
    }
}
