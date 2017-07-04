/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;

public class PrivateDockerHubRuntimeHandlerImpl implements RuntimeHandler {
    public static final String SERVER_ID_NOT_FOUND = "Server Id not found in settings.xml. ServerId=";

    private AbstractWebAppMojo mojo;

    public PrivateDockerHubRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws MojoExecutionException {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final Server server = Utils.getServer(mojo.getSettings(), containerSetting.getServerId());
        if (server == null) {
            throw new MojoExecutionException(SERVER_ID_NOT_FOUND + containerSetting.getServerId());
        }
        return mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withRegion(mojo.getRegion())
                .withNewResourceGroup(mojo.getResourceGroup())
                .withNewLinuxPlan(mojo.getPricingTier())
                .withPrivateDockerHubImage(containerSetting.getImageName())
                .withCredentials(server.getUsername(), server.getPassword());
    }

    @Override
    public WebApp.Update updateAppRuntime() throws MojoExecutionException {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final Server server = Utils.getServer(mojo.getSettings(), containerSetting.getServerId());
        if (server == null) {
            throw new MojoExecutionException(SERVER_ID_NOT_FOUND + containerSetting.getServerId());
        }
        return mojo.getWebApp().update()
                .withPrivateDockerHubImage(containerSetting.getImageName())
                .withCredentials(server.getUsername(), server.getPassword());
    }
}
