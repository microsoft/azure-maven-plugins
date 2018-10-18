/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.handlers.BaseRuntimeHandler;

public class LinuxRuntimeHandlerImplV2 extends BaseRuntimeHandler {
    public static class Builder extends BaseRuntimeHandler.Builder<Builder>{
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public LinuxRuntimeHandlerImplV2 build() {
            return new LinuxRuntimeHandlerImplV2(this);
        }
    }

    private LinuxRuntimeHandlerImplV2(final Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
            servicePlanResourceGroup, region, pricingTier, log, OperatingSystem.LINUX);
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withBuiltInImage(runtime.getLinuxRuntime());
    }

    @Override
    public Update updateAppRuntime(WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);
        return app.update().withBuiltInImage(runtime.getLinuxRuntime());
    }
}
