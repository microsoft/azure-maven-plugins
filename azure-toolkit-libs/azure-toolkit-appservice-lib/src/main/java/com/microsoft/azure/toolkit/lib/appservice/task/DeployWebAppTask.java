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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DeployWebAppTask extends AzureTask<WebAppBase<?, ?, ?>> {
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker webapp, " +
            "you can navigate to %s to access your docker webapp.";
    private static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    private static final String START_APP = "Starting Web App after deploying artifacts...";
    private static final String START_APP_DONE = "Successfully started Web App.";
    private static final int DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL = 5;
    private static final int DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES = 30;
    private static final int DEPLOYMENT_STATUS_DISPLAY_REFRESH_INTERVAL = 500;
    private static final String CLEAR_MESSAGE_STRING = StringUtils.repeat(StringUtils.SPACE, 100) + "\r";

    private final WebAppBase<?, ?, ?> webApp;
    private final List<WebAppArtifact> artifacts;
    private final boolean restartSite;
    private final boolean openStreamingLogOnFailure;
    private final Boolean waitDeploymentComplete;
    private final IAzureMessager messager;
    private final AtomicReference<KuduDeploymentResult> deploymentResultAtomicReference = new AtomicReference<>();
    @Setter
    private long deploymentStatusRefreshInterval = DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL;
    @Setter
    private long deploymentStatusMaxRefreshTimes = DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES;
    @Setter
    private PrintStream deploymentStatusStream;


    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts) {
        this(webApp, artifacts, false);
    }

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts, boolean restartSite) {
        this(webApp, artifacts, restartSite, null, false);
    }

    public DeployWebAppTask(WebAppBase<?, ?, ?> webApp, List<WebAppArtifact> artifacts, boolean restartSite, Boolean waitDeploymentComplete, boolean openStreamingLogOnFailure) {
        this.webApp = webApp;
        this.artifacts = artifacts;
        this.restartSite = restartSite;
        this.waitDeploymentComplete = waitDeploymentComplete;
        this.openStreamingLogOnFailure = openStreamingLogOnFailure;
        this.messager = AzureMessager.getMessager();
    }

    @Override
    @AzureOperation(name = "internal/webapp.deploy_app.app", params = {"this.webApp.getName()"})
    public WebAppBase<?, ?, ?> doExecute() {
        if (Objects.requireNonNull(webApp.getRuntime()).isDocker()) {
            this.messager.info(AzureString.format(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE, "https://" + webApp.getHostName()));
            return webApp;
        }
        this.messager.info(String.format(DEPLOY_START, webApp.getName()));
        deployArtifacts();
        startAppService(webApp);
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
        final boolean trackDeploymentStatus = isTrackDeploymentStatus();
        if (trackDeploymentStatus) {
            artifactsOneDeploy.forEach(resource -> deploymentResultAtomicReference.set(webApp.pushDeploy(resource.getDeployType(), resource.getFile(),
                DeployOptions.builder().path(resource.getPath()).restartSite(restartSite).trackDeployment(true).build())));
        } else {
            artifactsOneDeploy.forEach(resource -> webApp.deploy(resource.getDeployType(), resource.getFile(), DeployOptions.builder().path(resource.getPath()).restartSite(restartSite).build()));
        }
        if (!waitUntilDeploymentReady(trackDeploymentStatus, this.deploymentStatusRefreshInterval, this.deploymentStatusMaxRefreshTimes) && openStreamingLogOnFailure) {
            new StreamingLogTask(webApp).doExecute();
        }
        OperationContext.action().setTelemetryProperty("deploy-cost", String.valueOf(System.currentTimeMillis() - startTime));
    }

    public boolean waitUntilDeploymentReady(boolean trackDeploymentStatus, long deploymentStatusRefreshInterval, long deploymentStatusMaxRefreshTimes) {
        final String trackId = Optional.ofNullable(deploymentResultAtomicReference.get())
            .map(KuduDeploymentResult::getDeploymentId).orElse(null);
        if (!trackDeploymentStatus || StringUtils.isBlank(trackId)) {
            return false;
        }
        final AtomicReference<CsmDeploymentStatus> status = new AtomicReference<>(null);
        final Timer timer = Objects.isNull(deploymentStatusStream) ? null : new Timer();
        Optional.ofNullable(timer).ifPresent(t -> t.schedule(new TrackDeploymentStatusTask(status), 0, DEPLOYMENT_STATUS_DISPLAY_REFRESH_INTERVAL));
        final CsmDeploymentStatus result = Mono.fromCallable(() -> {
                final CsmDeploymentStatus deploymentStatus = webApp.getDeploymentStatus(trackId);
                status.set(deploymentStatus);
                return deploymentStatus;
            })
            .delayElement(Duration.ofSeconds(deploymentStatusRefreshInterval))
            .subscribeOn(Schedulers.boundedElastic())
            .repeat(deploymentStatusMaxRefreshTimes)
            .takeUntil(csmDeploymentStatus -> !csmDeploymentStatus.getStatus().isRunning())
            .blockLast();
        Optional.ofNullable(timer).ifPresent(Timer::cancel);
        final DeploymentBuildStatus buildStatus = Optional.ofNullable(result).map(CsmDeploymentStatus::getStatus).orElse(null);
        if (buildStatus == null || buildStatus.isSucceed()) {
            return true;
        } else if (buildStatus.isTimeout()) {
            AzureMessager.getMessager().warning("Resource deployed, but failed to get the deployment status as timeout");
        } else if (buildStatus.isRunning()) {
            AzureMessager.getMessager().warning("Resource deployed, but the deployment is still in process in Azure");
        } else if (buildStatus.isFailed()) {
            final String errorMessages = CollectionUtils.isNotEmpty(result.getErrors()) ?
                    result.getErrors().stream().map(ErrorEntity::getMessage).collect(Collectors.joining(StringUtils.LF)) : StringUtils.EMPTY;
            final String failedInstancesLogs = CollectionUtils.isEmpty(result.getFailedInstancesLogs()) ?
                    StringUtils.join(result.getFailedInstancesLogs(), StringUtils.LF) : StringUtils.EMPTY;
            throw new AzureToolkitRuntimeException(String.format("Failed to start app %s. %s %s", webApp.getName(), errorMessages, failedInstancesLogs));
        }
        return false;
    }

    private boolean isTrackDeploymentStatus() {
        if (BooleanUtils.isTrue(this.waitDeploymentComplete) && webApp.getFormalStatus().isStopped()) {
            messager.info("Skip waiting deployment status for stopped web app.");
            return false;
        }
        if (BooleanUtils.isTrue(this.waitDeploymentComplete) && Objects.requireNonNull(webApp.getRuntime()).isWindows()) {
            messager.warning("`waitDeploymentComplete` is not supported in Windows runtime, skip waiting for deployment status.");
            return false;
        }
        return Optional.ofNullable(this.waitDeploymentComplete).orElse(Objects.requireNonNull(webApp.getRuntime()).isLinux());
    }

    private static void startAppService(WebAppBase<?, ?, ?> target) {
        if (!target.getFormalStatus().isRunning()) {
            AzureMessager.getMessager().info(START_APP);
            target.start();
            AzureMessager.getMessager().info(START_APP_DONE);
        }
    }

    @RequiredArgsConstructor
    private class TrackDeploymentStatusTask extends TimerTask{
        private final AtomicReference<CsmDeploymentStatus> status;
        private final AtomicInteger times = new AtomicInteger(0);

        @Override
        public void run() {
            final StringBuilder message = new StringBuilder(getDeploymentStatus(status.get()));
            // add dot to indicate process is still running
            final int dotTimes = times.addAndGet(1) % 4;
            IntStream.range(0, dotTimes).forEach(i -> message.append("."));
            IntStream.range(dotTimes, 4).forEach(i -> message.append(" "));
            printMessage(message.toString());
        }

        private void printMessage(final String message) {
            if (Objects.isNull(deploymentStatusStream)) {
                return;
            }
            deploymentStatusStream.print(CLEAR_MESSAGE_STRING);
            deploymentStatusStream.print(message);
            deploymentStatusStream.print('\r');
            deploymentStatusStream.flush();
        }

        private String getDeploymentStatus(final CsmDeploymentStatus deploymentStatus) {
            if (Objects.isNull(deploymentStatus)) {
                return "Waiting for deployment";
            }
            final StringBuilder message = new StringBuilder(128);
            message.append(String.format("Status %s ", deploymentStatus.getStatus().getValue()));
            final int totalInstanceCount = deploymentStatus.getTotalInstanceCount();
            if (totalInstanceCount > 0) {
                final String instanceStatus = deploymentStatus.getNumberOfInstancesFailed() == 0 ?
                    String.format("(%d/%d)", deploymentStatus.getNumberOfInstancesSuccessful(), totalInstanceCount) :
                    String.format("(%d/%d, failed %d)", deploymentStatus.getNumberOfInstancesSuccessful(),
                        totalInstanceCount, deploymentStatus.getNumberOfInstancesFailed());
                message.append(instanceStatus);
            }
           return message.toString();
        }
    }
}
