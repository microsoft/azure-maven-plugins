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

    @Override
    protected void doExecute() throws Exception {
        getLog().info(WEBAPP_DEPLOY_START + getAppName() + APOSTROPHE);

        final DeployFacade facade = getDeployFacade();
        facade.setupRuntime()
                .applySettings()
                .commitChanges();

        facade.deployArtifacts();

        getLog().info(WEBAPP_DEPLOY_SUCCESS + getAppName());
    }

    protected DeployFacade getDeployFacade() {
        return getWebApp() == null ?
                new DeployFacadeImplWithCreate(this) :
                new DeployFacadeImplWithUpdate(this);
    }
}
