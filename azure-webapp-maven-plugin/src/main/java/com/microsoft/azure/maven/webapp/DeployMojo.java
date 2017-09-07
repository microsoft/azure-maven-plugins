/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    public static final String WEBAPP_DEPLOY_START = "Start deploying to Web App %s...";
    public static final String WEBAPP_DEPLOY_SUCCESS = "Successfully deployed Web App at https://%s.azurewebsites.net";

    @Override
    protected void doExecute() throws Exception {
        getLog().info(String.format(WEBAPP_DEPLOY_START, getAppName()));

        getDeployFacade().setupRuntime()
                .applySettings()
                .commitChanges()    // Create or update Web App first
                .deployArtifacts(); // Deploy artifacts

        getLog().info(String.format(WEBAPP_DEPLOY_SUCCESS, getAppName()));
    }

    protected DeployFacade getDeployFacade() throws Exception {
        return getWebApp() == null ?
                new DeployFacadeImplWithCreate(this) :
                new DeployFacadeImplWithUpdate(this);
    }
}
