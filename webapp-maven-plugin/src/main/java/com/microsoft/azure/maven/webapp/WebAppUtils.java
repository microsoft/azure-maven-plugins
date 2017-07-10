/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import org.apache.maven.plugin.MojoExecutionException;

public class WebAppUtils {
    public static final String CONTAINER_SETTING_NOT_APPLICABLE =
            "<containerSettings> is not applicable to Web App on Windows; " +
                    "please use <javaVersion> and <javaWebContainer> to configure your runtime.";
    public static final String JAVA_VERSION_NOT_APPLICABLE = "<javaVersion> is not applicable to Web App on Linux; " +
            "please use <containerSettings> to specify your runtime.";

    private static boolean isLinuxWebApp(final WebApp app) {
        return app.inner().kind().contains("linux");
    }

    public static void assureLinuxWebApp(final WebApp app) throws MojoExecutionException {
        if (!isLinuxWebApp(app)) {
            throw new MojoExecutionException(CONTAINER_SETTING_NOT_APPLICABLE);
        }
    }

    public static void assureWindowsWebApp(final WebApp app) throws MojoExecutionException {
        if (isLinuxWebApp(app)) {
            throw new MojoExecutionException(JAVA_VERSION_NOT_APPLICABLE);
        }
    }

    public static WebApp.DefinitionStages.WithNewAppServicePlan defineApp(final AbstractWebAppMojo mojo) {
        if (mojo.getAzureClient().resourceGroups().checkExistence(mojo.getResourceGroup())) {
            return mojo.getAzureClient().webApps()
                    .define(mojo.getAppName())
                    .withRegion(mojo.getRegion())
                    .withExistingResourceGroup(mojo.getResourceGroup());
        } else {
            return mojo.getAzureClient().webApps()
                    .define(mojo.getAppName())
                    .withRegion(mojo.getRegion())
                    .withNewResourceGroup(mojo.getResourceGroup());
        }
    }
}
