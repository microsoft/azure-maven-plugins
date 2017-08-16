/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import org.apache.maven.plugin.MojoExecutionException;

public class DeployFacadeImplWithUpdate extends DeployFacadeBaseImpl {
    private WebApp.Update update = null;

    public DeployFacadeImplWithUpdate(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public DeployFacadeBaseImpl setupRuntime() throws MojoExecutionException {
        try {
            update = getRuntimeHandler().updateAppRuntime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public DeployFacadeBaseImpl applySettings() throws MojoExecutionException {
        getSettingsHandler().processSettings(update);
        return this;
    }

    @Override
    public DeployFacadeBaseImpl commitChanges() throws MojoExecutionException {
        update.apply();
        return this;
    }
}
