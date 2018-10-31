/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import org.apache.maven.plugin.MojoExecutionException;

public class NullRuntimeHandlerImpl implements RuntimeHandler {
    public static final String NO_RUNTIME_CONFIG = "No runtime stack is specified in pom.xml; " +
            "use <javaVersion>, <linuxRuntime> or <containerSettings> to configure runtime stack.";

    @Override
    public WithCreate defineAppWithRuntime() throws Exception {
        throw new MojoExecutionException(NO_RUNTIME_CONFIG);
    }

    @Override
    public Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.clearTags(app);

        return app.update();
    }
}
