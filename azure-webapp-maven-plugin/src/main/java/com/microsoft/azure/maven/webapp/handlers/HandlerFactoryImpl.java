/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.handlers.ArtifactHandler;
import com.microsoft.azure.maven.handlers.RuntimeHandler;
import com.microsoft.azure.maven.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.maven.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.artifact.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.artifact.NONEArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.artifact.WarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.LinuxRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PrivateDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PrivateRegistryRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PublicDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.WebAppRuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.runtime.WindowsRuntimeHandlerImpl;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.util.Locale;

public class HandlerFactoryImpl extends HandlerFactory {
    public static final String UNKNOWN_DEPLOYMENT_TYPE =
        "The value of <deploymentType> is unknown, supported values are: jar, war, zip, ftp, auto and none.";

    @Override
    public RuntimeHandler getRuntimeHandler(final WebAppConfiguration config, final Azure azureClient,
                                            final Log log) throws MojoExecutionException {
        if (config.getOs() == null) {
            return new NullRuntimeHandlerImpl();
        }
        final WebAppRuntimeHandler.Builder builder;
        switch (config.getOs()) {
            case Windows:
                builder = new WindowsRuntimeHandlerImpl.Builder().javaVersion(config.getJavaVersion())
                    .webContainer(config.getWebContainer());
                break;
            case Linux:
                builder = new LinuxRuntimeHandlerImpl.Builder().runtime(config.getRuntimeStack());
                break;
            case Docker:
                builder = getDockerRuntimeHandlerBuilder(config);
                break;
            default:
                throw new MojoExecutionException("Unknown ");
        }
        return builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .log(log)
            .build();
    }

    protected WebAppRuntimeHandler.Builder getDockerRuntimeHandlerBuilder(final WebAppConfiguration config)
        throws MojoExecutionException {

        final WebAppRuntimeHandler.Builder<? extends WebAppRuntimeHandler.Builder> builder;
        final DockerImageType imageType = AppServiceUtils.getDockerImageType(config.getImage(), config.getServerId(),
            config.getRegistryUrl());
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                builder = new PublicDockerHubRuntimeHandlerImpl.Builder();
                break;
            case PRIVATE_DOCKER_HUB:
                builder = new PrivateDockerHubRuntimeHandlerImpl.Builder().mavenSettings(config.getMavenSettings());
                break;
            case PRIVATE_REGISTRY:
                builder = new PrivateRegistryRuntimeHandlerImpl.Builder().mavenSettings(config.getMavenSettings());
                break;
            default:
                throw new MojoExecutionException("Invalid docker runtime configured.");
        }
        builder.image(config.getImage()).serverId(config.getServerId()).registryUrl(config.getRegistryUrl());
        return builder;
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

    protected ArtifactHandler getV2ArtifactHandler(AbstractWebAppMojo mojo) {
        return new ArtifactHandlerImplV2.Builder()
            .project(mojo.getProject())
            .session(mojo.getSession())
            .filtering(mojo.getMavenResourcesFiltering())
            .resources(mojo.getDeployment().getResources())
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .log(mojo.getLog())
            .runtime(mojo.getRuntime())
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
}
