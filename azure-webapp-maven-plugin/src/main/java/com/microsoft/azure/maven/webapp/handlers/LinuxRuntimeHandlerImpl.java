/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import static com.microsoft.azure.maven.webapp.WebAppUtils.getLinuxRunTimeStack;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;

public class LinuxRuntimeHandlerImpl implements RuntimeHandler {

    private AbstractWebAppMojo mojo;

    public LinuxRuntimeHandlerImpl(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        return WebAppUtils.defineApp(mojo)
                .withNewLinuxPlan(mojo.getPricingTier())
                .withBuiltInImage(getLinuxRunTimeStack(mojo.getLinuxRuntime()));
    }

    @Override
    public Update updateAppRuntime(WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);

        return app.update().withBuiltInImage(getLinuxRunTimeStack(mojo.getLinuxRuntime()));
    }
}
