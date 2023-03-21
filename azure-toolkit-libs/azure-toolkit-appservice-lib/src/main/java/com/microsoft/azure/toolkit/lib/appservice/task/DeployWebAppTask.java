/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions;
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
import org.apache.commons.lang3.BooleanUtils;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DeployWebAppTask extends AzureTask<WebAppBase<?, ?, ?>> {
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker webapp, " +
            "you can navigate to %s to access your docker webapp.";
    private static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    private static final String START_APP = "Starting Web App after deploying artifacts...";
    private static final String START_APP_DONE = "Successfully started Web App.";
    private static final int DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL = 10;
    private static final int DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES = 20;

    private final WebAppBase<?, ?, ?> webApp;
    private final List<WebAppArtifact> artifacts;
    private final boolean restartSite;
    private final boolean openStreamingLogOnFailure;
    private final Boolean waitDeploymentComplete;
    private final IAzureMessager messager;
    private Disposable subscription;

    @Setter
    private long deploymentStatusRefreshInterval = DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL;
    @Setter
    private long deploymentStatusMaxRefreshTimes = DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES;

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
        if (webApp.getRuntime().isDocker()) {
            this.messager.info(AzureString.format(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE, "https://" + webApp.getHostName()));
            return webApp;
        }
        this.messager.info(String.format(DEPLOY_START, webApp.getName()));
        deployArtifacts();
        this.messager.info(String.format(DEPLOY_FINISH, webApp.getHostName()));
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
        artifactsOneDeploy.forEach(resource -> webApp.pushDeploy(resource.getDeployType(), resource.getFile(),
                DeployOptions.builder().path(resource.getPath()).restartSite(restartSite).trackDeployment(true).build()));
        if (BooleanUtils.isTrue(waitDeploymentComplete) && !webApp.waitUntilReady(DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL, DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES)) {
            startStreamingLog();
        }
        OperationContext.action().setTelemetryProperty("deploy-cost", String.valueOf(System.currentTimeMillis() - startTime));
    }

    private static void startAppService(WebAppBase<?, ?, ?> target) {
        if (!target.getFormalStatus().isRunning()) {
            AzureMessager.getMessager().info(START_APP);
            target.start();
            AzureMessager.getMessager().info(START_APP_DONE);
        }
    }

    private void startStreamingLog() {
        if (!webApp.isEnableWebServerLogging() || !openStreamingLogOnFailure) {
            return;
        }
        this.messager.info(AzureString.format("Opening streaming log of app({0})...", webApp.getName()));
        this.messager.debug("###############STREAMING LOG BEGIN##################");
        this.subscription = this.webApp.streamAllLogsAsync()
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
