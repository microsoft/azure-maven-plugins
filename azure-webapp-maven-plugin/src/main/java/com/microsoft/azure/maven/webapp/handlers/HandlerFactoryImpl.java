/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.maven.plugin.MojoExecutionException;

public class HandlerFactoryImpl extends HandlerFactory {
    public static final String RUNTIME_CONFIG_CONFLICT = "Conflict settings found. <javaVersion>, <linuxRuntime>" +
        "and <containerSettings> should not be set at the same time.";
    public static final String NO_RUNTIME_HANDLER = "Not able to process the runtime stack configuration; " +
        "please check <javaVersion>, <linuxRuntime> or <containerSettings> tag in pom.xml";
    public static final String IMAGE_NAME_MISSING = "<imageName> not found within <containerSettings> tag.";

    @Override
    public RuntimeHandler getRuntimeHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        final JavaVersion javaVersion = mojo.getJavaVersion();
        final String linuxRuntime = mojo.getLinuxRuntime();
        final ContainerSetting containerSetting = mojo.getContainerSettings();

        // No runtime setting is specified
        if (javaVersion == null && linuxRuntime == null && isContainerSettingEmpty(containerSetting)) {
            return new NullRuntimeHandlerImpl();
        }

        // Duplicated runtime are specified
        if (isDuplicatedRuntimeDefined(javaVersion, linuxRuntime, containerSetting)) {
            throw new MojoExecutionException(RUNTIME_CONFIG_CONFLICT);
        }

        if (javaVersion != null) {
            return new JavaRuntimeHandlerImpl(mojo);
        }

        if (linuxRuntime != null) {
            return new LinuxRuntimeHandlerImpl(mojo);
        }

        final DockerImageType imageType = WebAppUtils.getDockerImageType(containerSetting);
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                return new PublicDockerHubRuntimeHandlerImpl(mojo);
            case PRIVATE_DOCKER_HUB:
                return new PrivateDockerHubRuntimeHandlerImpl(mojo);
            case PRIVATE_REGISTRY:
                return new PrivateRegistryRuntimeHandlerImpl(mojo);
            case NONE:
                throw new MojoExecutionException(IMAGE_NAME_MISSING);
        }

        throw new MojoExecutionException(NO_RUNTIME_HANDLER);
    }

    @Override
    public SettingsHandler getSettingsHandler(final AbstractWebAppMojo mojo) {
        return new SettingsHandlerImpl(mojo);
    }

    @Override
    public ArtifactHandler getArtifactHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        return mojo.getDeploymentType().getArtifactHandlerFromMojo(mojo);
    }

    @Override
    public DeploymentSlotHandler getDeploymentSlotHandler(AbstractWebAppMojo mojo) {
        return new DeploymentSlotHandler(mojo);
    }

    private boolean isDuplicatedRuntimeDefined(final JavaVersion javaVersion, final String linuxRuntime,
            final ContainerSetting containerSetting) {
        return javaVersion != null ? linuxRuntime != null || !isContainerSettingEmpty(containerSetting)
                : linuxRuntime != null && !isContainerSettingEmpty(containerSetting);
    }

    private boolean isContainerSettingEmpty(final ContainerSetting containerSetting) {
        return containerSetting == null || containerSetting.isEmpty();
    }
}
