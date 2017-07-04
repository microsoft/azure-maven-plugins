/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.plugin.MojoExecutionException;

public class PublicDockerHubRuntimeHandlerImpl implements RuntimeHandler {
    private AbstractWebAppMojo mojo;

    public PublicDockerHubRuntimeHandlerImpl(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws MojoExecutionException {
        return mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withRegion(mojo.getRegion())
                .withNewResourceGroup(mojo.getResourceGroup())
                .withNewLinuxPlan(mojo.getPricingTier())
                .withPublicDockerHubImage(mojo.getContainerSettings().getImageName());
    }

    @Override
    public WebApp.Update updateAppRuntime() throws MojoExecutionException {
        final WebApp app = mojo.getWebApp();

        final ContainerSetting containerSetting = mojo.getContainerSettings();
        return app.update().withPublicDockerHubImage(containerSetting.getImageName());
    }
}
