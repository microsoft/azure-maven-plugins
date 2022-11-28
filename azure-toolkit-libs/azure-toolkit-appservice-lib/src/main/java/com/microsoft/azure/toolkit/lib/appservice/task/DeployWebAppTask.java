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
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DeployWebAppTask extends AzureTask<WebAppBase<?, ?, ?>> {
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker webapp, " +
            "you can navigate to %s to access your docker webapp.";
    private static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    private static final int DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL = 10;
    private static final int DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES = 20;

    private final WebAppBase<?, ?, ?> webApp;
    private final List<WebAppArtifact> artifacts;
    private final boolean restartSite;
    private final Boolean waitDeploymentComplete;
    private final IAzureMessager messager;

    @Setter
    private long deploymentStatusRefreshInterval = DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL;
    @Setter
    private long deploymentStatusMaxRefreshTimes = DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES;

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts) {
        this(webApp, artifacts, false);
    }

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts, boolean restartSite) {
        this(webApp, artifacts, restartSite, null);
    }

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts, boolean restartSite, Boolean waitDeploymentComplete) {
        this.webApp = webApp;
        this.artifacts = artifacts;
        this.restartSite = restartSite;
        this.waitDeploymentComplete = waitDeploymentComplete;
        this.messager = AzureMessager.getMessager();
    }

    @Override
    @AzureOperation(name = "webapp.deploy_app.app", params = {"this.webApp.getName()"}, type = AzureOperation.Type.SERVICE)
    public WebAppBase<?, ?, ?> doExecute() {
        if (webApp.getRuntime().isDocker()) {
            this.messager.info(AzureString.format(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE, "https://" + webApp.getHostName()));
            return webApp;
        }
        this.messager.info(String.format(DEPLOY_START, webApp.getName()));
        deployArtifacts();
        this.messager.info(String.format(DEPLOY_FINISH, webApp.getHostName()));
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
        if (isWaitDeploymentComplete()) {
            final AtomicReference<KuduDeploymentResult> reference = new AtomicReference<>();
            artifactsOneDeploy.forEach(resource -> reference.set(webApp.pushDeploy(resource.getDeployType(), resource.getFile(),
                    DeployOptions.builder().path(resource.getPath()).restartSite(restartSite).trackDeployment(true).build())));
            trackDeployment(webApp, reference);
        } else {
            artifactsOneDeploy.forEach(resource -> webApp.deploy(resource.getDeployType(), resource.getFile(),
                    DeployOptions.builder().path(resource.getPath()).restartSite(restartSite).build()));
        }
        OperationContext.action().setTelemetryProperty("deploy-cost", String.valueOf(System.currentTimeMillis() - startTime));
    }

    private boolean isWaitDeploymentComplete() {
        if (webApp.getFormalStatus().isStopped()) {
            messager.info("Skip waiting deployment status for stopped web app.");
            return false;
        }
        if (webApp.getRuntime().isWindows() && BooleanUtils.isTrue(this.waitDeploymentComplete)) {
            messager.warning("Wait deployment complete is not supported in Windows runtime, skip waiting for deployment status.");
            return false;
        }
        return Optional.ofNullable(this.waitDeploymentComplete).orElseGet(() -> webApp.getRuntime().isLinux());
    }

    private void trackDeployment(final WebAppBase<?, ?, ?> target, final AtomicReference<KuduDeploymentResult> resultReference) {
        final KuduDeploymentResult kuduDeploymentResult = resultReference.get();
        if (kuduDeploymentResult == null) {
            return;
        }
        final CsmDeploymentStatus status = Mono.fromCallable(() -> getDeploymentStatus(target, kuduDeploymentResult))
                .delayElement(Duration.ofSeconds(deploymentStatusRefreshInterval))
                .subscribeOn(Schedulers.boundedElastic())
                .repeat(deploymentStatusMaxRefreshTimes)
                .takeUntil(csmDeploymentStatus -> !csmDeploymentStatus.getStatus().isRunning())
                .blockLast();
        final DeploymentBuildStatus buildStatus = status.getStatus();
        if (buildStatus.isTimeout()) {
            this.messager.warning("Resource deployed, but failed to get the deployment status as timeout");
        } else if (buildStatus.isRunning()) {
            this.messager.warning("Resource deployed, but the deployment is still in process in Azure");
        } else if (buildStatus.isFailed()) {
            final String errorMessages = CollectionUtils.isNotEmpty(status.getErrors()) ?
                    status.getErrors().stream().map(ErrorEntity::getMessage).collect(Collectors.joining(StringUtils.LF)) : StringUtils.EMPTY;
            final String failedInstancesLogs = CollectionUtils.isEmpty(status.getFailedInstancesLogs()) ?
                    StringUtils.join(status.getFailedInstancesLogs(), StringUtils.LF) : StringUtils.EMPTY;
            throw new AzureToolkitRuntimeException(String.format("Failed to deploy the artifact to %s. %s %s", target.getName(), errorMessages, failedInstancesLogs));
        }
    }

    private CsmDeploymentStatus getDeploymentStatus(final WebAppBase<?, ?, ?> target, final KuduDeploymentResult result) {
        final CsmDeploymentStatus deploymentStatus = target.getDeploymentStatus(result.getDeploymentId());
        if (Objects.isNull(deploymentStatus)) {
            return null;
        }
        final String statusMessage = String.format("Deployment Status: %s; Successful Instance Count: %s; In-progress Instance Count: %s; Failed Instance Count: %s",
                deploymentStatus.getStatus().getValue(), deploymentStatus.getNumberOfInstancesSuccessful(), deploymentStatus.getNumberOfInstancesInProgress(), deploymentStatus.getNumberOfInstancesFailed());
        this.messager.info(statusMessage);
        return deploymentStatus;
    }

}
