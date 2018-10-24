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
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.v1.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.LinuxRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.NONEArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.PrivateDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.PrivateRegistryRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.PublicDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.WarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.WindowsRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v2.ArtifactHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.v2.LinuxRuntimeHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.v2.PrivateDockerHubRuntimeHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.v2.PrivateRegistryRuntimeHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.v2.PublicDockerHubRuntimeHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.v2.WindowsRuntimeHandlerImplV2;
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
    public RuntimeHandler getRuntimeHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException,
        AzureAuthFailureException {

        switch (SchemaVersion.fromString(mojo.getSchemaVersion())) {
            case V1:
                return getV1RuntimeHandler(mojo);
            case V2:
                return getV2RuntimeHandler(mojo);
            default:
                throw new MojoExecutionException(SchemaVersion.UNKNOWN_SCHEMA_VERSION);
        }
    }

    protected RuntimeHandler getV1RuntimeHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
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
        } else if (linuxRuntime != null) {
            return new LinuxRuntimeHandlerImpl(mojo);
        } else {
            return getV1DockerRuntimeHandler(containerSetting.getImageName(), containerSetting.getServerId(),
                containerSetting.getRegistryUrl(), mojo);
        }
    }

    protected RuntimeHandler getV2RuntimeHandler(final AbstractWebAppMojo mojo)
        throws MojoExecutionException, AzureAuthFailureException {

        assureV2RequiredPropertyConfigured(mojo);

        final BaseRuntimeHandler.Builder builder;
        final RuntimeSetting runtime = mojo.getRuntime();
        if (StringUtils.isEmpty(runtime.getOs()) || null == runtime.getJavaVersion()) {
            // todo, add the guidance here
            throw new MojoExecutionException("Incorrect <runtime> settings in pom.xml, please correct it.");
        }

        switch (OperatingSystemEnum.fromString(runtime.getOs())) {
            case Windows:
                builder = new WindowsRuntimeHandlerImplV2.Builder();
                break;
            case Linux:
                builder = new LinuxRuntimeHandlerImplV2.Builder();
                break;
            case Docker:
                builder = getV2DockerRuntimeHandlerBuilder(mojo);
                break;
            default:
                throw new MojoExecutionException(
                    "The value of <os> is unknown, supported values are: windows, linux and docker.");
        }
        return builder.runtime(runtime)
            .appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
            .build();
    }

    private void assureV2RequiredPropertyConfigured(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        if (StringUtils.isEmpty(mojo.getRegion())) {
            throw new MojoExecutionException("No <region> is specified, please configure it in pom.xml.");
        }
    }

    protected RuntimeHandler getV1DockerRuntimeHandler(final String imageName, final String serverId,
                                                       final String registryUrl, final AbstractWebAppMojo mojo)
        throws MojoExecutionException {

        final DockerImageType imageType = WebAppUtils.getDockerImageType(imageName, serverId, registryUrl);
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                return new PublicDockerHubRuntimeHandlerImpl(mojo);
            case PRIVATE_DOCKER_HUB:
                return new PrivateDockerHubRuntimeHandlerImpl(mojo);
            case PRIVATE_REGISTRY:
                return new PrivateRegistryRuntimeHandlerImpl(mojo);
            case NONE:
                throw new MojoExecutionException(IMAGE_NAME_MISSING);
            default:
                throw new MojoExecutionException(NO_RUNTIME_HANDLER);
        }
    }

    protected BaseRuntimeHandler.Builder getV2DockerRuntimeHandlerBuilder(final AbstractWebAppMojo mojo)
        throws MojoExecutionException {

        final RuntimeSetting runtime = mojo.getRuntime();
        final DockerImageType imageType = WebAppUtils.getDockerImageType(runtime.getImage(), runtime.getServerId(),
            runtime.getRegistryUrl());

        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                return new PublicDockerHubRuntimeHandlerImplV2.Builder();
            case PRIVATE_DOCKER_HUB:
                final PrivateDockerHubRuntimeHandlerImplV2.Builder privateDockerHubRuntimeHandlerImplV2Builder =
                    new PrivateDockerHubRuntimeHandlerImplV2.Builder();
                privateDockerHubRuntimeHandlerImplV2Builder.mavenSettings(mojo.getSettings());
                return privateDockerHubRuntimeHandlerImplV2Builder;
            case PRIVATE_REGISTRY:
                final PrivateRegistryRuntimeHandlerImplV2.Builder privateRegistryRuntimeHandlerImplV2Builder =
                    new PrivateRegistryRuntimeHandlerImplV2.Builder();
                privateRegistryRuntimeHandlerImplV2Builder.mavenSettings(mojo.getSettings());
                return privateRegistryRuntimeHandlerImplV2Builder;
            case NONE:
                throw new MojoExecutionException(
                    "The configuration <image> is not specified within <runtime>, please configure it in pom.xml.");
            default:
                throw new MojoExecutionException("Configuration <runtime> is not correct. Please fix it in pom.xml.");
        }
    }

    @Override
    public SettingsHandler getSettingsHandler(final AbstractWebAppMojo mojo) {
        return new SettingsHandlerImpl(mojo);
    }

    @Override
    public ArtifactHandler getArtifactHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
        switch (SchemaVersion.fromString(mojo.getSchemaVersion())) {
            case V1:
                return getV1ArtifactHandler(mojo);
            case V2:
                return getV2ArtifactHandler(mojo);
            default:
                throw new MojoExecutionException(SchemaVersion.UNKNOWN_SCHEMA_VERSION);
        }
    }

    protected ArtifactHandler getV1ArtifactHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException {
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

    protected ArtifactHandler getV2ArtifactHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
        assureV2RequiredPropertyConfigured(mojo);
        return new ArtifactHandlerImplV2(mojo);
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
