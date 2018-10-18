/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.handlers.BaseRuntimeHandler;

public class WindowsRuntimeHandlerImplV2 extends BaseRuntimeHandler {
    public static class Builder extends BaseRuntimeHandler.Builder<WindowsRuntimeHandlerImplV2.Builder>{
        @Override
        protected WindowsRuntimeHandlerImplV2.Builder self() {
            return this;
        }

        @Override
        public WindowsRuntimeHandlerImplV2 build() {
            return new WindowsRuntimeHandlerImplV2(this);
        }
    }

    private WindowsRuntimeHandlerImplV2(final WindowsRuntimeHandlerImplV2.Builder builder) {
        super(builder);
    }

    @Override
    public WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
            servicePlanResourceGroup, region, pricingTier, log, OperatingSystem.WINDOWS);
        final WithCreate withCreate = WebAppUtils.defineWindowsApp(resourceGroup, appName, azure, plan);
        withCreate.withJavaVersion(runtime.getWindowsJavaVersion()).withWebContainer(runtime.getWebContainer());
        return withCreate;
    }

    @Override
    public Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.assureWindowsWebApp(app);
        WebAppUtils.clearTags(app);
        final Update update = app.update();
        update.withJavaVersion(runtime.getWindowsJavaVersion()).withWebContainer(runtime.getWebContainer());
        return update;
    }

}
