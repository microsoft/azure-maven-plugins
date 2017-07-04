/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.plugin.MojoExecutionException;

public class JavaRuntimeHandlerImpl implements RuntimeHandler {
    private AbstractWebAppMojo mojo;

    public JavaRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws MojoExecutionException {
        final WebApp.DefinitionStages.WithCreate withCreate = mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withRegion(mojo.getRegion())
                .withNewResourceGroup(mojo.getResourceGroup())
                .withNewWindowsPlan(mojo.getPricingTier());
        withCreate.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return withCreate;
    }

    @Override
    public WebApp.Update updateAppRuntime() throws MojoExecutionException {
        final WebApp.Update update = mojo.getWebApp().update();
        update.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return update;
    }
}
