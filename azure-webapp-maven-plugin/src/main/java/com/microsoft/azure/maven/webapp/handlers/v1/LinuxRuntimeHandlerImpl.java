/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v1;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;

import static com.microsoft.azure.maven.webapp.WebAppUtils.getLinuxRunTimeStack;

public class LinuxRuntimeHandlerImpl implements RuntimeHandler {

    private AbstractWebAppMojo mojo;

    public LinuxRuntimeHandlerImpl(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = WebAppUtils.createOrGetAppServicePlan(mojo.getAppServicePlanName(),
            mojo.getResourceGroup(), mojo.getAzureClient(), mojo.getAppServicePlanResourceGroup(),
            mojo.getRegion(), mojo.getPricingTier(), mojo.getLog(), OperatingSystem.LINUX);

        return WebAppUtils.defineLinuxApp(mojo.getResourceGroup(), mojo.getAppName(), mojo.getAzureClient(), plan)
                .withBuiltInImage(getLinuxRunTimeStack(mojo.getLinuxRuntime()));
    }

    @Override
    public Update updateAppRuntime(WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);

        return app.update().withBuiltInImage(getLinuxRunTimeStack(mojo.getLinuxRuntime()));
    }
}
