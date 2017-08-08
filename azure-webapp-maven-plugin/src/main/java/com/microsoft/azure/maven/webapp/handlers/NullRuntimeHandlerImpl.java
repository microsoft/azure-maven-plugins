/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.plugin.MojoExecutionException;

public class NullRuntimeHandlerImpl implements RuntimeHandler {
    public static final String NO_RUNTIME_CONFIG = "No runtime stack is specified in pom.xml; " +
            "use <javaVersion> or <containerSettings> to configure runtime stack.";
    private AbstractWebAppMojo mojo;

    public NullRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws MojoExecutionException {
        throw new MojoExecutionException(NO_RUNTIME_CONFIG);
    }

    @Override
    public WebApp.Update updateAppRuntime() throws MojoExecutionException {
        return mojo.getWebApp().update();
    }
}
