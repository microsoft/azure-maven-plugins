/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import org.apache.maven.model.Resource;

import java.util.List;

public abstract class DeployFacadeBaseImpl implements DeployFacade {
    public static final String NO_RESOURCES_CONFIG = "No resources specified in pom.xml. Skip artifacts deployment.";
    public static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    public static final String START_APP = "Starting Web App after deploying artifacts...";
    public static final String STOP_APP_DONE = "Successfully stopped Web App.";
    public static final String START_APP_DONE = "Successfully started Web App.";

    private AbstractWebAppMojo mojo;

    private DeploymentUtil util;

    public DeployFacadeBaseImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
        this.util = new DeploymentUtil();
    }

    public abstract DeployFacade setupRuntime() throws Exception;

    public abstract DeployFacade applySettings() throws Exception;

    public abstract DeployFacade commitChanges() throws Exception;

    public DeployFacade deployArtifacts() throws Exception {
        final List<Resource> resources = getMojo().getResources();
        if (resources == null || resources.isEmpty()) {
            logInfo(NO_RESOURCES_CONFIG);
        } else {
            try {
                util.beforeDeployArtifacts();

                HandlerFactory.getInstance()
                        .getArtifactHandler(getMojo())
                        .publish(resources);
            } finally {
                util.afterDeployArtifacts();
            }
        }
        return this;
    }

    protected AbstractWebAppMojo getMojo() {
        return mojo;
    }

    protected void logInfo(final String message) {
        getMojo().getLog().info(message);
    }

    class DeploymentUtil {
        boolean isAppStopped = false;

        public void beforeDeployArtifacts() throws Exception {
            if (getMojo().isStopAppDuringDeployment()) {
                logInfo(STOP_APP);

                getMojo().getWebApp().stop();
                isAppStopped = true;

                logInfo(STOP_APP_DONE);
            }
        }

        public void afterDeployArtifacts() throws Exception {
            if (isAppStopped) {
                logInfo(START_APP);

                getMojo().getWebApp().start();
                isAppStopped = false;

                logInfo(START_APP_DONE);
            }
        }
    }
}
