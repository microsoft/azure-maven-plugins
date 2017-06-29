/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.webapp.handlers.DeployHandler;
import com.microsoft.azure.maven.webapp.handlers.PrivateDockerHubDeployHandler;
import com.microsoft.azure.maven.webapp.handlers.PrivateDockerRegistryDeployHandler;
import com.microsoft.azure.maven.webapp.handlers.PublicDockerHubDeployHandler;
import com.microsoft.azure.maven.telemetry.TelemetryEvent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.HashMap;

/**
 * Goal which deploy specified docker image to a Linux web app in Azure.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    public static final String APOSTROPHE = "...";

    public static final String WEBAPP_DEPLOY_START = "Start deploying to Web App ";
    public static final String WEBAPP_DEPLOY_SUCCESS = "Successfully deployed to Web App ";
    public static final String WEBAPP_DEPLOY_FAILURE = "Failed to deploy to Web App ";
    public static final String FAILURE_REASON = "failureReason";

    public static final String CONTAINER_SETTING_NOT_FOUND = "No configuration for containerSetting found.";
    public static final String DEPLOY_HANDLER_NOT_FOUND = "Not able to handle deployment for such configuration.";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        deploy();
    }

    /**
     * Deploy web app.
     *
     * @throws MojoExecutionException
     */
    protected void deploy() throws MojoExecutionException {
        getTelemetryProxy().trackEvent(TelemetryEvent.DEPLOY_START);
        getLog().info(WEBAPP_DEPLOY_START + getAppName() + APOSTROPHE);

        // Get a DeployHandler
        final DeployHandler handler = getDeployHandler();
        if (handler == null) {
            processError(DEPLOY_HANDLER_NOT_FOUND);
            return;
        }

        // Validate Web App
        final WebApp app = getWebApp();
        OperationResult result = handler.validate(app);
        if (!result.isSuccess()) {
            processError(result.getMessage());
            return;
        }

        // Invoke deployment
        result = handler.deploy(app);
        if (!result.isSuccess()) {
            processError(result.getMessage());
        } else {
            getTelemetryProxy().trackEvent(TelemetryEvent.DEPLOY_SUCCESS);
            getLog().info(WEBAPP_DEPLOY_SUCCESS + getAppName());
        }
    }

    /**
     * Create DeployHandler based on configuration.
     *
     * @return A new DeployHandler instance or null.
     */
    protected DeployHandler getDeployHandler() {
        if (containerSetting == null || containerSetting.isEmpty()) {
            getLog().info(CONTAINER_SETTING_NOT_FOUND);
            return null;
        }

        // Public Docker Hub image
        if (Utils.isStringEmpty(containerSetting.getServerId())) {
            return new PublicDockerHubDeployHandler(this);
        }

        // Private Docker Hub image
        if (Utils.isStringEmpty(containerSetting.getRegistryUrl())) {
            return new PrivateDockerHubDeployHandler(this);
        }

        // Private Docker registry image
        return new PrivateDockerRegistryDeployHandler(this);
    }

    private void processError(final String errorMessage) throws MojoExecutionException {
        final HashMap<String, String> failureReason = new HashMap<>();
        failureReason.put(FAILURE_REASON, errorMessage);
        getTelemetryProxy().trackEvent(TelemetryEvent.DEPLOY_FAILURE, failureReason);
        getLog().error(WEBAPP_DEPLOY_FAILURE + getAppName());
        if (isFailingOnError()) {
            throw new MojoExecutionException(errorMessage);
        } else {
            getLog().error(errorMessage);
        }
    }
}
