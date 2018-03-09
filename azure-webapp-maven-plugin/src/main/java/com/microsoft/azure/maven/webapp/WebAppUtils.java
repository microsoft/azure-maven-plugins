/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithNewAppServicePlan;
import com.microsoft.azure.management.resources.fluentcore.arm.models.GroupableResource.DefinitionStages.WithGroup;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
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

    public static WithNewAppServicePlan defineApp(final AbstractWebAppMojo mojo) throws Exception {
        final WithGroup<WithNewAppServicePlan> withGroup = mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withRegion(mojo.getRegion());
        final String resourceGroup = mojo.getResourceGroup();
        return mojo.getAzureClient().resourceGroups().checkExistence(resourceGroup) ?
                withGroup.withExistingResourceGroup(resourceGroup) :
                withGroup.withNewResourceGroup(resourceGroup);
    }

    public static DockerImageType getDockerImageType(final ContainerSetting containerSetting) {
        if (containerSetting == null || StringUtils.isEmpty(containerSetting.getImageName())) {
            return DockerImageType.NONE;
        }

        if (containerSetting.isUseBuiltinImage()) {
            return DockerImageType.BUILT_IN;
        }

        final boolean isCustomRegistry = StringUtils.isNotEmpty(containerSetting.getRegistryUrl());
        final boolean isPrivate = StringUtils.isNotEmpty(containerSetting.getServerId());

        if (isCustomRegistry) {
            return isPrivate ? DockerImageType.PRIVATE_REGISTRY : DockerImageType.UNKNOWN;
        } else {
            return isPrivate ? DockerImageType.PRIVATE_DOCKER_HUB : DockerImageType.PUBLIC_DOCKER_HUB;
        }
    }
}
