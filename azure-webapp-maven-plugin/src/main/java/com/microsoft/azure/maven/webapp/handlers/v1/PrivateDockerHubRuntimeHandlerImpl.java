/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v1;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;

public class PrivateDockerHubRuntimeHandlerImpl implements RuntimeHandler {
    public static final String SERVER_ID_NOT_FOUND = "Server Id not found in settings.xml. ServerId=";

    private AbstractWebAppMojo mojo;

    public PrivateDockerHubRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WithCreate defineAppWithRuntime() throws Exception {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final Server server = Utils.getServer(mojo.getSettings(), containerSetting.getServerId());
        if (server == null) {
            throw new MojoExecutionException(SERVER_ID_NOT_FOUND + containerSetting.getServerId());
        }

        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(mojo.getAppServicePlanName(),
            mojo.getResourceGroup(), mojo.getAzureClient(), mojo.getAppServicePlanResourceGroup(),
            mojo.getRegion(), mojo.getPricingTier(), mojo.getLog(), OperatingSystem.LINUX);
        return WebAppUtils.defineLinuxApp(mojo.getResourceGroup(), mojo.getAppName(), mojo.getAzureClient(), plan)
                .withPrivateDockerHubImage(containerSetting.getImageName())
                .withCredentials(server.getUsername(), server.getPassword());
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);

        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final Server server = Utils.getServer(mojo.getSettings(), containerSetting.getServerId());
        if (server == null) {
            throw new MojoExecutionException(SERVER_ID_NOT_FOUND + containerSetting.getServerId());
        }
        return app.update()
                .withPrivateDockerHubImage(containerSetting.getImageName())
                .withCredentials(server.getUsername(), server.getPassword());
    }
}
