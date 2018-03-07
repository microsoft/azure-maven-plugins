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
    public static final String RUNTIME_CONFIG_CONFLICT = "<javaVersion> is for Web App on Windows; " +
            "<containerSettings> is for Web App on Linux; they can't be specified at the same time.";
    public static final String NO_RUNTIME_HANDLER = "Not able to process the runtime stack configuration; " +
            "please check <javaVersion> or <containerSettings> tag in pom.xml";
    public static final String IMAGE_NAME_MISSING = "<imageName> not found within <containerSettings> tag.";
    public static final String DEPLOYMENT_TYPE_NOT_FOUND = "<deploymentType> is not configured.";
    public static final String UNKNOWN_DEPLOYMENT_TYPE = "Unknown value from <deploymentType> tag.";

    @Override
    public RuntimeHandler getRuntimeHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        final JavaVersion javaVersion = mojo.getJavaVersion();
        final ContainerSetting containerSetting = mojo.getContainerSettings();

        // Neither <javaVersion> nor <containerSettings> is specified
        if (javaVersion == null && (containerSetting == null || containerSetting.isEmpty())) {
            return new NullRuntimeHandlerImpl();
        }

        // Both <javaVersion> and <containerSettings> are specified
        if (javaVersion != null && containerSetting != null && !containerSetting.isEmpty()) {
            throw new MojoExecutionException(RUNTIME_CONFIG_CONFLICT);
        }

        if (javaVersion != null) {
            return new JavaRuntimeHandlerImpl(mojo);
        }

        final DockerImageType imageType = WebAppUtils.getDockerImageType(containerSetting);
        switch (imageType) {
            case BUILT_IN:
                return new BuiltInImageRuntimeHandlerImpl(mojo);
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
    public SettingsHandler getSettingsHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        return new SettingsHandlerImpl(mojo);
    }

    @Override
    public ArtifactHandler getArtifactHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        switch (mojo.getDeploymentType()) {
            case NONE:
                throw new MojoExecutionException(DEPLOYMENT_TYPE_NOT_FOUND);
            case UNKNOWN:
                throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
            case FTP:
            default:
                return new FTPArtifactHandlerImpl(mojo);
        }
    }
}
