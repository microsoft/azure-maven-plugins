/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DeployWebAppTask extends AzureTask<IWebAppBase<?>> {
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker webapp, " +
        "you can navigate to %s to access your docker webapp.";
    private static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    private static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    private static final String START_APP = "Starting Web App after deploying artifacts...";
    private static final String STOP_APP_DONE = "Successfully stopped Web App.";
    private static final String START_APP_DONE = "Successfully started Web App.";
    private static final String RUNNING = "Running";
    private final IWebAppBase<?> webApp;
    private final List<WebAppArtifact> artifacts;
    private final boolean isStopAppDuringDeployment;

    public DeployWebAppTask(IWebAppBase<?> webApp, List<WebAppArtifact> artifacts) {
        this(webApp, artifacts, false);
    }

    public DeployWebAppTask(IWebAppBase<?> webApp, List<WebAppArtifact> artifacts, boolean isStopAppDuringDeployment) {
        this.webApp = webApp;
        this.artifacts = artifacts;
        this.isStopAppDuringDeployment = isStopAppDuringDeployment;
    }

    @Override
    @AzureOperation(name = "webapp.deploy", params = {"this.webApp.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public IWebAppBase<?> execute() {
        if (webApp.getRuntime().isDocker()) {
            AzureMessager.getMessager().info(AzureString.format(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE, "https://" + webApp.hostName()));
            return webApp;
        }
        try {
            AzureMessager.getMessager().info(String.format(DEPLOY_START, webApp.name()));
            if (isStopAppDuringDeployment) {
                stopAppService(webApp);
            }
            deployArtifacts();
            AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, webApp.hostName()));
        } finally {
            startAppService(webApp);
        }
        return webApp;
    }

    private void deployArtifacts() {
        if (artifacts.stream().anyMatch(artifact -> artifact.getDeployType() == null)) {
            throw new AzureToolkitRuntimeException("missing deployment type for some artifacts.");
        }

        final List<WebAppArtifact> artifactsOneDeploy = this.artifacts.stream()
            .filter(artifact -> artifact.getDeployType() != null)
            .collect(Collectors.toList());
        artifactsOneDeploy.forEach(resource -> webApp.deploy(resource.getDeployType(), resource.getFile(), resource.getPath()));
    }

    private static void stopAppService(IWebAppBase<?> target) {
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

    private static void startAppService(IWebAppBase<?> target) {
        if (!StringUtils.equalsIgnoreCase(target.state(), RUNNING)) {
            AzureMessager.getMessager().info(START_APP);
            target.start();
            AzureMessager.getMessager().info(START_APP_DONE);
        }
    }

}
