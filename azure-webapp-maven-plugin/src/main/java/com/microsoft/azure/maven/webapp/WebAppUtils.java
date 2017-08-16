/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

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

    public static WebApp.DefinitionStages.WithNewAppServicePlan defineApp(final AbstractWebAppMojo mojo)
            throws Exception {
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

    public static boolean isPublicDockerHubImage(final ContainerSetting containerSetting) {
        return StringUtils.isEmpty(containerSetting.getServerId()) &&
                StringUtils.isEmpty(containerSetting.getRegistryUrl());
    }

    public static boolean isPrivateDockerHubImage(final ContainerSetting containerSetting) {
        return StringUtils.isNotEmpty(containerSetting.getServerId()) &&
                StringUtils.isEmpty(containerSetting.getRegistryUrl());
    }

    public static boolean isPrivateRegistryImage(final ContainerSetting containerSetting) {
        return StringUtils.isNotEmpty(containerSetting.getServerId()) &&
                StringUtils.isNotEmpty(containerSetting.getRegistryUrl());
    }

    /**
     * Work Around:
     * When a web app is created from Azure Portal, there are hidden tags associated with the app.
     * It will be messed up when calling "update" API.
     * An issue is logged at https://github.com/Azure/azure-sdk-for-java/issues/1755 .
     * Remove all tags here to make it work.
     *
     * @param app
     */
    public static void clearTags(final WebApp app) {
        app.inner().withTags(null);
    }
}
