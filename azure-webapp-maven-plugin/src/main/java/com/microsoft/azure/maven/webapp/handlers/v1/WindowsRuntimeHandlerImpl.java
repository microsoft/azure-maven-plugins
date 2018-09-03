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
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;

public class WindowsRuntimeHandlerImpl implements RuntimeHandler {
    private AbstractWebAppMojo mojo;

    public WindowsRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(mojo.getAppServicePlanName(),
            mojo.getResourceGroup(), mojo.getAzureClient(), mojo.getAppServicePlanResourceGroup(),
            mojo.getRegion(), mojo.getPricingTier(), mojo.getLog(), OperatingSystem.WINDOWS);
        final WithCreate withCreate = WebAppUtils.defineWindowsApp(mojo.getResourceGroup(), mojo.getAppName(),
            mojo.getAzureClient(), plan);

        withCreate.withJavaVersion(mojo.getJavaVersion()).withWebContainer(mojo.getJavaWebContainer());
        return withCreate;
    }

    @Override
    public Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.assureWindowsWebApp(app);
        WebAppUtils.clearTags(app);

        final Update update = app.update();
        update.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return update;
    }
}
