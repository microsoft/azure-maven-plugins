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
import org.apache.maven.plugin.MojoExecutionException;

public class HandlerFactoryImpl extends HandlerFactory {
    public static final String RUNTIME_CONFIG_CONFLICT = "<javaVersion> is for Web App on Windows; " +
            "<containerSettings> is for Web App on Linux; they can't be specified at the same time.";
    public static final String NO_RUNTIME_HANDLER = "Not able to process the runtime stack configuration; " +
            "please check <javaVersion> or <containerSettings> tag.";

    @Override
    public RuntimeHandler getRuntimeHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
        final JavaVersion javaVersion = mojo.getJavaVersion();
        final ContainerSetting containerSetting = mojo.getContainerSettings();

        // Neither <javaVersion> nor <containerSettings> is specified
        if (javaVersion == null && (containerSetting == null || containerSetting.isEmpty())) {
            return new NullRuntimeHandlerImpl(mojo);
        }

        // Both <javaVersion> and <containerSettings> are specified
        if (javaVersion != null && containerSetting != null && !containerSetting.isEmpty()) {
            throw new MojoExecutionException(RUNTIME_CONFIG_CONFLICT);
        }

        if (javaVersion != null) {
            return new JavaRuntimeHandlerImpl(mojo);
        }

        if (WebAppUtils.isPublicDockerHubImage(containerSetting)) {
            return new PublicDockerHubRuntimeHandlerImpl(mojo);
        }

        if (WebAppUtils.isPrivateDockerHubImage(containerSetting)) {
            return new PrivateDockerHubRuntimeHandlerImpl(mojo);
        }

        if (WebAppUtils.isPrivateRegistryImage(containerSetting)) {
            return new PrivateRegistryRuntimeHandlerImpl(mojo);
        }

        throw new MojoExecutionException(NO_RUNTIME_HANDLER);
    }

    @Override
    public SettingsHandler getSettingsHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
        return new SettingsHandlerImpl(mojo);
    }

    @Override
    public ArtifactHandler getArtifactHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
        switch (mojo.getDeploymentType()) {
            case FTP:
            default:
                return new FTPArtifactHandlerImpl(mojo);
        }
    }
}
