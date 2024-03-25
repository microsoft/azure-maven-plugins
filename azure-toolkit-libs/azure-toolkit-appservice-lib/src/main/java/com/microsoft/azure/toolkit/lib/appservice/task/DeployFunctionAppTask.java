/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;
import reactor.core.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class DeployFunctionAppTask extends AzureTask<FunctionAppBase<?, ?, ?>> {

    private static final String RUNNING = "Running";
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";
    private static final String DEPLOY_START = "Starting deployment...";
    private static final String DEPLOY_FINISH = "Deployment succeed";
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker app service";
    private static final String FAILED_TO_LIST_TRIGGERS = "Deployment succeeded, but failed to list http trigger urls.";
    private final FunctionAppBase<?, ?, ?> target;
    private final File stagingDirectory;
    private final FunctionDeployType deployType;
    private final IAzureMessager messager;
    private Disposable subscription;
    private final boolean openStreamingLogOnFailure;

    public DeployFunctionAppTask(@Nonnull FunctionAppBase<?, ?, ?> target, @Nonnull File stagingFolder, @Nullable FunctionDeployType deployType) {
        this(target, stagingFolder, deployType, false);
    }

    public DeployFunctionAppTask(@Nonnull FunctionAppBase<?, ?, ?> target, @Nonnull File stagingFolder,
                                 @Nullable FunctionDeployType deployType,
                                 boolean openStreamingLogOnFailure) {
        this.target = target;
        this.stagingDirectory = stagingFolder;
        this.deployType = deployType;
        this.messager = AzureMessager.getMessager();
        this.openStreamingLogOnFailure = openStreamingLogOnFailure;
    }

    @Override
    public AzureString getDescription() {
        return AzureString.format("Deploy artifact to function app %s", target.getName());
    }

    @Override
    public FunctionAppBase<?, ?, ?> doExecute() {
        final Boolean isDockerRuntime = Optional.ofNullable(target.getRuntime()).map(Runtime::isDocker).orElse(false);
        if (isDockerRuntime) {
            messager.info(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE);
            return target;
        }
        deployArtifact();
        if (target instanceof FunctionApp && openStreamingLogOnFailure) {
            try {
                ((FunctionApp) target).listHTTPTriggerUrls();
            } catch (final Exception e) {
                // show warning instead of exception for list triggers
                messager.warning(FAILED_TO_LIST_TRIGGERS);
                new StreamingLogTask(target).doExecute();
            }
        }
        return target;
    }

    private void deployArtifact() {
        messager.info(DEPLOY_START);
        OperationContext.action().setTelemetryProperty("subscriptionId", target.getSubscriptionId());
        OperationContext.action().setTelemetryProperty("deployType", Optional.ofNullable(deployType).map(FunctionDeployType::toString).orElse("empty"));
        OperationContext.action().setTelemetryProperty("deployToSlot", String.valueOf(target instanceof FunctionAppDeploymentSlot));
        Optional.ofNullable(target.getRuntime()).ifPresent(runtime -> OperationContext.action().setTelemetryProperty("runtime", runtime.getDisplayName()));
        Optional.ofNullable(target.getRuntime()).map(Runtime::getOperatingSystem).ifPresent(os -> OperationContext.action().setTelemetryProperty("os", os.getValue()));
        Optional.ofNullable(target.getRuntime()).map(Runtime::getJavaVersionUserText).ifPresent(javaVersion -> OperationContext.action().setTelemetryProperty("javaVersion", javaVersion));
        Optional.ofNullable(target.getAppServicePlan()).map(AppServicePlan::getPricingTier).ifPresent(pricingTier -> OperationContext.action().setTelemetryProperty("pricingTier", pricingTier.getSize()));

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
        if (target instanceof FunctionApp) {
            final List<FunctionEntity> triggers = ((FunctionApp) target).listFunctions(true);
            final Action<AppServiceAppBase<?, ?, ?>> streamingLog = Optional.ofNullable(AzureActionManager.getInstance().getAction(AppServiceAppBase.START_STREAM_LOG))
                    .map(action -> action.bind(target)).orElse(null);
            final List<Action<?>> actions = triggers.stream().map(trigger -> {
                if (trigger.isHttpTrigger()) {
                    return Optional.ofNullable(AzureActionManager.getInstance().getAction(FunctionEntity.TRIGGER_FUNCTION_IN_BROWSER))
                        .map(action -> action.bind(trigger).withLabel(String.format("Trigger \"%s\"", trigger.getName()))).orElse(null);
                } else {
                    return Optional.ofNullable(AzureActionManager.getInstance().getAction(FunctionEntity.TRIGGER_FUNCTION))
                        .map(action -> action.bind(trigger)
                            .withLabel(String.format("Trigger \"%s\"", trigger.getName()))).orElse(null);
                }
            }).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedList::new));
            Optional.ofNullable(streamingLog).ifPresent(action -> actions.add(0, action));
            messager.info(String.format(DEPLOY_FINISH), actions.toArray());
        } else {
            messager.info(String.format(DEPLOY_FINISH));
        }
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

}
