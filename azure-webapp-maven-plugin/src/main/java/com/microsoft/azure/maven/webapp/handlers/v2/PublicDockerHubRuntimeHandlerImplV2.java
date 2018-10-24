/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.handlers.BaseRuntimeHandler;

public class PublicDockerHubRuntimeHandlerImplV2 extends BaseRuntimeHandler {
    public static class Builder extends BaseRuntimeHandler.Builder<PublicDockerHubRuntimeHandlerImplV2.Builder>{
        @Override
        protected PublicDockerHubRuntimeHandlerImplV2.Builder self() {
            return this;
        }

        @Override
        public PublicDockerHubRuntimeHandlerImplV2 build() {
            return new PublicDockerHubRuntimeHandlerImplV2(this);
        }
    }

    private PublicDockerHubRuntimeHandlerImplV2(final PublicDockerHubRuntimeHandlerImplV2.Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
            servicePlanResourceGroup, region, pricingTier, log, OperatingSystem.LINUX);
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withPublicDockerHubImage(runtime.getImage());
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);
        return app.update().withPublicDockerHubImage(runtime.getImage());
    }
}
