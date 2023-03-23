/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


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
        if (Objects.requireNonNull(target.getRuntime()).isDocker()) {
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
                startStreamingLog();
            }
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
        messager.info(String.format(DEPLOY_FINISH));
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



    private void startStreamingLog() {
        if (!target.isStreamingLogSupported() || !openStreamingLogOnFailure) {
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

    private synchronized void stopStreamingLog() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
