/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
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
import com.microsoft.azure.maven.webapp.handlers.v1.NONEArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.WarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v2.ArtifactHandlerImplV2;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.util.Locale;

import static com.microsoft.azure.maven.webapp.WebAppUtils.getLinuxRunTimeStack;

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

    protected RuntimeHandler getV1RuntimeHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException,
        AzureAuthFailureException {

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
        final BaseRuntimeHandler.Builder builder;

        if (javaVersion != null) {
            builder = new WindowsRuntimeHandlerImpl.Builder();
            builder.javaVersion(mojo.getJavaVersion()).webContainer(mojo.getJavaWebContainer());
        } else if (linuxRuntime != null) {
            builder = new LinuxRuntimeHandlerImpl.Builder();
            builder.runtime(getLinuxRunTimeStack(mojo.getLinuxRuntime()));
        } else {
            builder = getV1DockerRuntimeHandlerBuilder(mojo);
        }

        return builder.appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
            .build();
    }

    protected RuntimeHandler getV2RuntimeHandler(final AbstractWebAppMojo mojo)
        throws MojoExecutionException, AzureAuthFailureException {

        assureV2RequiredPropertyConfigured(mojo);

        final BaseRuntimeHandler.Builder builder;
        final RuntimeSetting runtime = mojo.getRuntime();
        // todo validate configuration

        switch (OperatingSystemEnum.fromString(runtime.getOs())) {
            case Windows:
                builder = new WindowsRuntimeHandlerImpl.Builder();
                builder.javaVersion(runtime.getJavaVersion()).webContainer(runtime.getWebContainer());
                break;
            case Linux:
                builder = new LinuxRuntimeHandlerImpl.Builder();
                builder.runtime(runtime.getLinuxRuntime());
                break;
            case Docker:
                builder = getV2DockerRuntimeHandlerBuilder(mojo);
                break;
            default:
                throw new MojoExecutionException(
                    "The value of <os> is unknown, the supported values are: windows, linux and docker.");
        }
        return builder.appName(mojo.getAppName())
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

    protected BaseRuntimeHandler.Builder getV1DockerRuntimeHandlerBuilder(final AbstractWebAppMojo mojo)
        throws MojoExecutionException {

        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final DockerImageType imageType = WebAppUtils.getDockerImageType(containerSetting.getImageName(),
            containerSetting.getServerId(), containerSetting.getRegistryUrl());

        final BaseRuntimeHandler.Builder builder;
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                builder = new PublicDockerHubRuntimeHandlerImpl.Builder();
                break;
            case PRIVATE_DOCKER_HUB:
                builder = new PrivateDockerHubRuntimeHandlerImpl.Builder();
                builder.mavenSettings(mojo.getSettings());
                break;
            case PRIVATE_REGISTRY:
                builder = new PrivateRegistryRuntimeHandlerImpl.Builder();
                builder.mavenSettings(mojo.getSettings());
                break;
            case NONE:
                throw new MojoExecutionException(IMAGE_NAME_MISSING);
            default:
                throw new MojoExecutionException(NO_RUNTIME_HANDLER);
        }
        return builder.image(containerSetting.getImageName())
            .serverId(containerSetting.getServerId())
            .registryUrl(containerSetting.getRegistryUrl());
    }

    protected BaseRuntimeHandler.Builder getV2DockerRuntimeHandlerBuilder(final AbstractWebAppMojo mojo)
        throws MojoExecutionException {

        final RuntimeSetting runtime = mojo.getRuntime();
        final DockerImageType imageType = WebAppUtils.getDockerImageType(runtime.getImage(), runtime.getServerId(),
            runtime.getRegistryUrl());

        final BaseRuntimeHandler.Builder builder;
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                builder = new PublicDockerHubRuntimeHandlerImpl.Builder();
                break;
            case PRIVATE_DOCKER_HUB:
                builder = new PrivateDockerHubRuntimeHandlerImpl.Builder();
                builder.mavenSettings(mojo.getSettings());
                break;
            case PRIVATE_REGISTRY:
                builder = new PrivateRegistryRuntimeHandlerImpl.Builder();
                builder.mavenSettings(mojo.getSettings());
                break;
            case NONE:
                throw new MojoExecutionException(
                    "The configuration <image> is not specified within <runtime>, please configure it in pom.xml.");
            default:
                throw new MojoExecutionException("Configuration <runtime> is not correct. Please fix it in pom.xml.");
        }
        return builder.image(runtime.getImage()).serverId(runtime.getServerId()).registryUrl(runtime.getRegistryUrl());
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
        final ArtifactHandlerBase.Builder builder;

        switch (mojo.getDeploymentType()) {
            case FTP:
                builder = new FTPArtifactHandlerImpl.Builder();
                break;
            case ZIP:
                builder = new ZIPArtifactHandlerImpl.Builder();
                break;
            case JAR:
                builder = new JarArtifactHandlerImpl.Builder().jarFile(mojo.getJarFile())
                    .linuxRuntime(mojo.getLinuxRuntime());
                break;
            case WAR:
                builder = new WarArtifactHandlerImpl.Builder().warFile(mojo.getWarFile())
                    .contextPath(mojo.getPath());
                break;
            case NONE:
                builder = new NONEArtifactHandlerImpl.Builder();
                break;
            case EMPTY:
            case AUTO:
                builder = getArtifactHandlerBuilderFromPackaging(mojo);
                break;
            default:
                throw new MojoExecutionException(DeploymentType.UNKNOWN_DEPLOYMENT_TYPE);
        }
        return builder.project(mojo.getProject())
            .session(mojo.getSession())
            .filtering(mojo.getMavenResourcesFiltering())
            .resources(mojo.getResources())
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .buildDirectoryAbsolutePath(mojo.getBuildDirectoryAbsolutePath())
            .log(mojo.getLog())
            .build();
    }

    protected ArtifactHandler getV2ArtifactHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
        assureV2RequiredPropertyConfigured(mojo);
        return new ArtifactHandlerImplV2.Builder()
            .project(mojo.getProject())
            .session(mojo.getSession())
            .filtering(mojo.getMavenResourcesFiltering())
            .resources(mojo.getDeployment().getResources())
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .log(mojo.getLog())
            .build();
    }

    protected ArtifactHandlerBase.Builder getArtifactHandlerBuilderFromPackaging(final AbstractWebAppMojo mojo)
        throws MojoExecutionException {
        String packaging = mojo.getProject().getPackaging();
        if (StringUtils.isEmpty(packaging)) {
            throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
        packaging = packaging.toLowerCase(Locale.ENGLISH).trim();
        switch (packaging) {
            case "war":
                return new WarArtifactHandlerImpl.Builder().warFile(mojo.getWarFile())
                    .contextPath(mojo.getPath());
            case "jar":
                return new JarArtifactHandlerImpl.Builder().jarFile(mojo.getJarFile())
                    .linuxRuntime(mojo.getLinuxRuntime());
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
