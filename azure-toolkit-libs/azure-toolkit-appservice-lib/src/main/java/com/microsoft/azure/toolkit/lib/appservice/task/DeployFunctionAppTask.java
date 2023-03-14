/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.task;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;
import reactor.core.Disposable;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.ANONYMOUS;

public class DeployFunctionAppTask extends AzureTask<FunctionAppBase<?, ?, ?>> {

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

    private final FunctionAppBase<?, ?, ?> target;
    private final File stagingDirectory;
    private final FunctionDeployType deployType;
    private final IAzureMessager messager;
    private Disposable subscription;
    private final boolean startStreamingLog;

    public DeployFunctionAppTask(@Nonnull FunctionAppBase<?, ?, ?> target, @Nonnull File stagingFolder, @Nullable FunctionDeployType deployType) {
        this(target, stagingFolder, deployType, AzureMessager.getMessager(), false);
    }

    public DeployFunctionAppTask(@Nonnull FunctionAppBase<?, ?, ?> target, @Nonnull File stagingFolder,
                                 @Nullable FunctionDeployType deployType, @Nonnull IAzureMessager messager,
                                 boolean startStreamingLog) {
        this.target = target;
        this.stagingDirectory = stagingFolder;
        this.deployType = deployType;
        this.messager = messager;
        this.startStreamingLog = startStreamingLog;
    }

    @Override
    public AzureString getDescription() {
        return AzureString.format("Deploy artifact to function app %s", target.getName());
    }

    @Override
    public FunctionAppBase<?, ?, ?> doExecute() {
        if (Objects.requireNonNull(target.getRuntime()).isDocker()) {
            messager.info(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE);
            return target;
        }
        deployArtifact();
        if (target instanceof FunctionApp) {
            listHTTPTriggerUrls((FunctionApp) target);
        }
        return target;
    }

    private void deployArtifact() {
        messager.info(DEPLOY_START);
        // For ftp deploy, we need to upload entire staging directory not the zipped package
        final File file = deployType == FunctionDeployType.FTP ? stagingDirectory : packageStagingDirectory();
        final long startTime = System.currentTimeMillis();
        if (deployType == null) {
            target.deploy(file);
        } else {
            target.deploy(file, deployType);
        }
        OperationContext.action().setTelemetryProperty("deploy-cost", String.valueOf(System.currentTimeMillis() - startTime));
        if (!StringUtils.equalsIgnoreCase(target.getStatus(), RUNNING)) {
            target.start();
        }
        messager.info(String.format(DEPLOY_FINISH, target.getHostName()));
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
                    StringUtils.equalsIgnoreCase(bindingResource.getTrigger().getProperty(AUTH_LEVEL), ANONYMOUS))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
                messager.info(NO_ANONYMOUS_HTTP_TRIGGER);
                return;
            }
            messager.info(HTTP_TRIGGER_URLS);
            anonymousTriggers.forEach(trigger -> messager.info(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())));
            if (anonymousTriggers.size() < httpFunction.size()) {
                messager.info(UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS);
            }
        } catch (final RuntimeException | InterruptedException e) {
            // show warning instead of exception for list triggers
            messager.warning(FAILED_TO_LIST_TRIGGERS);
            startStreamingLog();
        }
    }

    // todo: move to app service library
    // Refers https://github.com/Azure/azure-functions-core-tools/blob/3.0.3568/src/Azure.Functions.Cli/Actions/AzureActions/PublishFunctionAppAction.cs#L452
    private void syncTriggers(final FunctionApp functionApp) throws InterruptedException {
        messager.info(SYNC_TRIGGERS);
        Thread.sleep(5 * 1000);
        Mono.fromRunnable(() -> {
                try {
                    functionApp.syncTriggers();
                } catch (ManagementException e) {
                    if (e.getResponse().getStatusCode() != 200) { // Java SDK throw exception with 200 response, swallow exception in this case
                        throw e;
                    }
                }
            }).subscribeOn(Schedulers.boundedElastic())
            .retryWhen(Retry.fixedDelay(SYNC_FUNCTION_MAX_ATTEMPTS - 1, Duration.ofSeconds(SYNC_FUNCTION_DELAY))).block();
    }

    private List<FunctionEntity> listFunctions(final FunctionApp functionApp) {
        final int[] count = {0};
        return Mono.fromCallable(() -> {
                final AzureString message = count[0]++ == 0 ? AzureString.fromString(LIST_TRIGGERS) : AzureString.format(LIST_TRIGGERS_WITH_RETRY, count[0], LIST_TRIGGERS_MAX_RETRY);
                messager.info(message);
                return Optional.of(functionApp.listFunctions())
                    .filter(CollectionUtils::isNotEmpty)
                    .orElseThrow(() -> new AzureToolkitRuntimeException(NO_TRIGGERS_FOUNDED));
            }).subscribeOn(Schedulers.boundedElastic())
            .retryWhen(Retry.fixedDelay(LIST_TRIGGERS_MAX_RETRY - 1, Duration.ofSeconds(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS))).block();
    }

    private void startStreamingLog() {
        if (!target.isEnableWebServerLogging() || !startStreamingLog) {
            return;
        }
        final OperatingSystem operatingSystem = Optional.ofNullable(target.getRuntime()).map(Runtime::getOperatingSystem).orElse(null);
        if (operatingSystem == OperatingSystem.LINUX) {
            return;
        }
        messager.debug("###############STREAMING LOG BEGIN##################");
        subscription = target.streamAllLogsAsync()
                .doFinally((type) -> messager.debug("###############STREAMING LOG END##################"))
                .subscribe(messager::debug);
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (final Exception ignored) {
        } finally {
            stopStreamingLog();
        }
    }

    private void stopStreamingLog() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
