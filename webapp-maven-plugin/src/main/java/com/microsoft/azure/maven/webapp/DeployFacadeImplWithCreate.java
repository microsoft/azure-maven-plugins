/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import org.apache.maven.plugin.MojoExecutionException;

public class DeployFacadeImplWithCreate extends DeployFacadeBaseImpl {
    private WebApp.DefinitionStages.WithCreate withCreate = null;

    public DeployFacadeImplWithCreate(final AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public DeployFacadeBaseImpl setupRuntime() throws MojoExecutionException {
        withCreate = getRuntimeHandler().defineAppWithRunTime();
        return this;
    }

    @Override
    public DeployFacadeBaseImpl applySettings() throws MojoExecutionException {
        getSettingsHandler().processSettings(withCreate);
        return this;
    }

    @Override
    public DeployFacadeBaseImpl commitChanges() throws MojoExecutionException {
        withCreate.create();
        return this;
    }
}
