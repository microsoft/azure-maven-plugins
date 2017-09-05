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

    private AbstractWebAppMojo mojo;

    public DeployFacadeBaseImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public abstract DeployFacade setupRuntime() throws Exception;

    public abstract DeployFacade applySettings() throws Exception;

    public abstract DeployFacade commitChanges() throws Exception;

    public DeployFacade deployArtifacts() throws Exception {
        final List<Resource> resources = getMojo().getResources();
        if (resources == null || resources.isEmpty()) {
            getMojo().getLog().info(NO_RESOURCES_CONFIG);
        } else {
            beforeDeployArtifacts();

            HandlerFactory.getInstance()
                    .getArtifactHandler(getMojo())
                    .publish(resources);

            afterDeployArtifacts();
        }
        return this;
    }

    protected AbstractWebAppMojo getMojo() {
        return mojo;
    }

    protected void beforeDeployArtifacts() throws Exception {
        if (getMojo().isStopAppDuringDeployment()) {
            getMojo().getWebApp().stop();
        }
    }

    protected void afterDeployArtifacts() throws Exception {
        if (getMojo().isStopAppDuringDeployment()) {
            getMojo().getWebApp().restart();
        }
    }
}
