/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.appservice.model.CsmDeploymentStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions;
import com.microsoft.azure.toolkit.lib.appservice.model.DeploymentBuildStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.ErrorEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.KuduDeploymentResult;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeployWebAppTask extends AzureTask<WebAppBase<?, ?, ?>> {
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker webapp, " +
        "you can navigate to %s to access your docker webapp.";
    private static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    private static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    private static final String START_APP = "Starting Web App after deploying artifacts...";
    private static final String STOP_APP_DONE = "Successfully stopped Web App.";
    private static final String START_APP_DONE = "Successfully started Web App.";
    private static final String RUNNING = "Running";
    private final WebAppBase<?, ?, ?> webApp;
    private final List<WebAppArtifact> artifacts;
    private final boolean isStopAppDuringDeployment;

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts) {
        this(webApp, artifacts, false);
    }

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts, boolean isStopAppDuringDeployment) {
        this.webApp = webApp;
        this.artifacts = artifacts;
        this.isStopAppDuringDeployment = isStopAppDuringDeployment;
    }

    @Override
    @AzureOperation(name = "webapp.deploy_app.app", params = {"this.webApp.getName()"}, type = AzureOperation.Type.SERVICE)
    public WebAppBase<?, ?, ?> doExecute() {
        if (webApp.getRuntime().isDocker()) {
            AzureMessager.getMessager().info(AzureString.format(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE, "https://" + webApp.getHostName()));
            return webApp;
        }
        try {
            AzureMessager.getMessager().info(String.format(DEPLOY_START, webApp.name()));
            if (isStopAppDuringDeployment) {
                stopAppService(webApp);
            }
            deployArtifacts();
            AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, webApp.getHostName()));
        } finally {
            startAppService(webApp);
        }
        return webApp;
    }

    private void deployArtifacts() {
        if (artifacts.stream().anyMatch(artifact -> artifact.getDeployType() == null)) {
            throw new AzureToolkitRuntimeException("missing deployment type for some artifacts.");
        }
        final long startTime = System.currentTimeMillis();
        final List<WebAppArtifact> artifactsOneDeploy = this.artifacts.stream()
            .filter(artifact -> artifact.getDeployType() != null)
            .collect(Collectors.toList());
        final AtomicReference<KuduDeploymentResult> reference = new AtomicReference<>();
        artifactsOneDeploy.forEach(resource ->
                reference.set(webApp.pushDeploy(resource.getDeployType(), resource.getFile(), DeployOptions.builder().path(resource.getPath()).build())));
        trackDeployment(reference);
        OperationContext.action().setTelemetryProperty("deploy-cost", String.valueOf(System.currentTimeMillis() - startTime));
    }

    private void trackDeployment(final AtomicReference<KuduDeploymentResult> resultReference) {
        final KuduDeploymentResult kuduDeploymentResult = resultReference.get();
        if (kuduDeploymentResult == null) {
            return;
        }
        final CsmDeploymentStatus status = Mono.fromCallable(() -> webApp.getDeploymentStatus(kuduDeploymentResult.getDeploymentId()))
                .delayElement(Duration.ofSeconds(1))
                .subscribeOn(Schedulers.boundedElastic())
                .repeat()
                .takeUntil(csmDeploymentStatus -> !csmDeploymentStatus.getStatus().isRunning())
                .blockLast();
        final DeploymentBuildStatus buildStatus = status.getStatus();
        if (buildStatus.isSucceed()) {
            return;
        } else if (buildStatus.isTimeout()) {
            AzureMessager.getMessager().warning("Deploy succeed, but failed to get the deployment status");
        } else if (status.getStatus().isFailed()) {
            final String failedInstancesLogs = CollectionUtils.isEmpty(status.getFailedInstancesLogs()) ?
                    StringUtils.join(status.getFailedInstancesLogs(), StringUtils.LF) : StringUtils.EMPTY;
            final String errorMessages = CollectionUtils.isNotEmpty(status.getErrors()) ?
                    status.getErrors().stream().map(ErrorEntity::getMessage).collect(Collectors.joining(StringUtils.LF)) : StringUtils.EMPTY;
            throw new AzureToolkitRuntimeException(String.join(StringUtils.LF, errorMessages, errorMessages));
        }
    }

    private static void stopAppService(WebAppBase<?, ?, ?> target) {
        AzureMessager.getMessager().info(STOP_APP);
        target.stop();
        // workaround for the resources release problem.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/191
        try {
            TimeUnit.SECONDS.sleep(10 /* 10 seconds */);
        } catch (InterruptedException e) {
            // swallow exception
        }
        AzureMessager.getMessager().info(STOP_APP_DONE);
    }

    private static void startAppService(WebAppBase<?, ?, ?> target) {
        if (!StringUtils.equalsIgnoreCase(target.getStatus(), RUNNING)) {
            AzureMessager.getMessager().info(START_APP);
            target.start();
            AzureMessager.getMessager().info(START_APP_DONE);
        }
    }

}
