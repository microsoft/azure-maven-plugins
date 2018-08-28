/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.artifacthandler.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.artifacthandler.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import com.microsoft.azure.maven.webapp.handlers.v1.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.LinuxRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.NONEArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.PrivateDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.PrivateRegistryRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.PublicDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.WarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.WindowsRuntimeHandlerImpl;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public class HandlerFactoryImpl extends HandlerFactory {
    public static final String RUNTIME_CONFIG_CONFLICT = "Conflict settings found. <javaVersion>, <linuxRuntime>" +
        "and <containerSettings> should not be set at the same time.";
    public static final String NO_RUNTIME_HANDLER = "Not able to process the runtime stack configuration; " +
        "please check <javaVersion>, <linuxRuntime> or <containerSettings> tag in pom.xml";
    public static final String IMAGE_NAME_MISSING = "<imageName> not found within <containerSettings> tag.";
    public static final String UNKNOWN_DEPLOYMENT_TYPE =
        "The value of <deploymentType> is unknown, supported values are: jar, war, zip, ftp, auto and none.";

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
            return new WindowsRuntimeHandlerImpl(mojo);
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
        switch (mojo.getDeploymentType()) {
            case FTP:
                return new FTPArtifactHandlerImpl(mojo);
            case ZIP:
                return new ZIPArtifactHandlerImpl(mojo);
            case JAR:
                return new JarArtifactHandlerImpl(mojo);
            case WAR:
                return new WarArtifactHandlerImpl(mojo);
            case NONE:
                return new NONEArtifactHandlerImpl(mojo);
            case EMPTY:
            case AUTO:
                return getArtifactHandlerFromPackaging(mojo);
            default:
                throw new MojoExecutionException(DeploymentType.UNKNOWN_DEPLOYMENT_TYPE);
        }
    }

    protected ArtifactHandler getArtifactHandlerFromPackaging(final AbstractWebAppMojo mojo)
        throws MojoExecutionException {
        String packaging = mojo.getProject().getPackaging();
        if (StringUtils.isEmpty(packaging)) {
            throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
        packaging = packaging.toLowerCase(Locale.ENGLISH).trim();
        switch (packaging) {
            case "war":
                return new WarArtifactHandlerImpl(mojo);
            case "jar":
                return new JarArtifactHandlerImpl(mojo);
            default:
                throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
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
