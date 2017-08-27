/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import org.apache.maven.plugin.MojoExecutionException;

public class DeployFacadeImplWithCreate extends DeployFacadeBaseImpl {
    public static final String WEBAPP_NOT_EXIST = "Target Web App doesn't exist. Creating a new one...";
    public static final String WEBAPP_CREATED = "Successfully created Web App";
    private WithCreate withCreate = null;

    public DeployFacadeImplWithCreate(final AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public DeployFacadeBaseImpl setupRuntime() throws MojoExecutionException {
        try {
            withCreate = HandlerFactory.getInstance()
                    .getRuntimeHandler(getMojo())
                    .defineAppWithRunTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public DeployFacadeBaseImpl applySettings() throws MojoExecutionException {
        HandlerFactory.getInstance()
                .getSettingsHandler(getMojo())
                .processSettings(withCreate);
        return this;
    }

    @Override
    public DeployFacadeBaseImpl commitChanges() throws MojoExecutionException {
        getMojo().getLog().info(WEBAPP_NOT_EXIST);
        withCreate.create();
        getMojo().getLog().info(WEBAPP_CREATED);
        return this;
    }
}
