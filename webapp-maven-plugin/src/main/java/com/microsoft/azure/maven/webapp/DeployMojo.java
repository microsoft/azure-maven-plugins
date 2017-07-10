/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.telemetry.TelemetryEvent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        try {
            logDeployStart();

            final DeployFacade facade = getDeployFacade();
            facade.setupRuntime()
                    .applySettings()
                    .commitChanges();

            facade.deployArtifacts();

            logDeploySuccess();
        } catch (Exception e) {
            processException(e);
        }
    }

    protected DeployFacade getDeployFacade() {
        return getWebApp() == null ?
                new DeployFacadeImplWithCreate(this) :
                new DeployFacadeImplWithUpdate(this);
    }

    private void processException(final Exception exception) throws MojoExecutionException {
        String message = exception.getMessage();
        if (StringUtils.isEmpty(message)) {
            message = exception.toString();
        }
        logDeployFailure(message);

        if (isFailingOnError()) {
            throw new MojoExecutionException(message, exception);
        } else {
            getLog().error(message);
        }
    }

    private void logDeployStart() {
        getTelemetryProxy().trackEvent(TelemetryEvent.DEPLOY_START);
        getLog().info(WEBAPP_DEPLOY_START + getAppName() + APOSTROPHE);
    }

    private void logDeploySuccess() {
        getTelemetryProxy().trackEvent(TelemetryEvent.DEPLOY_SUCCESS);
        getLog().info(WEBAPP_DEPLOY_SUCCESS + getAppName());
    }

    private void logDeployFailure(final String message) {
        final HashMap<String, String> failureReason = new HashMap<>();
        failureReason.put(FAILURE_REASON, message);

        getTelemetryProxy().trackEvent(TelemetryEvent.DEPLOY_FAILURE, failureReason);
        getLog().error(WEBAPP_DEPLOY_FAILURE + getAppName());
    }
}
