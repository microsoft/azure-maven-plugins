/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import org.apache.maven.plugin.MojoExecutionException;

public class JavaRuntimeHandlerImpl implements RuntimeHandler {
    public static final String WEB_CONTAINER_NOT_CONFIGURED = "<javaWebContainer> is not configured.";

    private AbstractWebAppMojo mojo;

    public JavaRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws MojoExecutionException {
        validate();

        final WebApp.DefinitionStages.WithCreate withCreate = WebAppUtils.defineApp(mojo)
                .withNewWindowsPlan(mojo.getPricingTier());

        withCreate.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return withCreate;
    }

    @Override
    public WebApp.Update updateAppRuntime() throws MojoExecutionException {
        validate();

        final WebApp app = mojo.getWebApp();
        WebAppUtils.assureWindowsWebApp(app);

        final WebApp.Update update = app.update();
        update.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return update;
    }

    private void validate() throws MojoExecutionException {
        if (mojo.getJavaWebContainer() == null) {
            throw new MojoExecutionException(WEB_CONTAINER_NOT_CONFIGURED);
        }
    }
}
