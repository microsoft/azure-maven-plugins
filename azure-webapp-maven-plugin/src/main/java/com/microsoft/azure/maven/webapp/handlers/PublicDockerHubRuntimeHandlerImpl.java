/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;

public class PublicDockerHubRuntimeHandlerImpl implements RuntimeHandler {
    private AbstractWebAppMojo mojo;

    public PublicDockerHubRuntimeHandlerImpl(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws Exception {
        return WebAppUtils.defineApp(mojo)
                .withNewLinuxPlan(mojo.getPricingTier())
                .withPublicDockerHubImage(mojo.getContainerSettings().getImageName());
    }

    @Override
    public WebApp.Update updateAppRuntime() throws Exception {
        final WebApp app = mojo.getWebApp();
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);

        final ContainerSetting containerSetting = mojo.getContainerSettings();
        return app.update().withPublicDockerHubImage(containerSetting.getImageName());
    }
}
