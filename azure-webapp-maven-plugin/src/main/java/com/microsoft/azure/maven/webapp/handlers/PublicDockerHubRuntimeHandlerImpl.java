/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.WebAppUtils;

public class PublicDockerHubRuntimeHandlerImpl extends BaseRuntimeHandler {
    public static class Builder extends BaseRuntimeHandler.Builder<PublicDockerHubRuntimeHandlerImpl.Builder>{
        @Override
        protected PublicDockerHubRuntimeHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public PublicDockerHubRuntimeHandlerImpl build() {
            return new PublicDockerHubRuntimeHandlerImpl(this);
        }
    }

    private PublicDockerHubRuntimeHandlerImpl(final PublicDockerHubRuntimeHandlerImpl.Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
            servicePlanResourceGroup, region, pricingTier, log, OperatingSystem.LINUX);
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withPublicDockerHubImage(image);
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);
        return app.update().withPublicDockerHubImage(image);
    }
}
