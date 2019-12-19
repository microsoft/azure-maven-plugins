/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.docker.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.handlers.ArtifactHandler;
import com.microsoft.azure.maven.handlers.RuntimeHandler;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerImplV2;
import com.microsoft.azure.maven.webapp.handlers.runtime.LinuxRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PrivateDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PrivateRegistryRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PublicDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.WebAppRuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.runtime.WindowsRuntimeHandlerImpl;

public class HandlerFactoryImpl extends HandlerFactory {
	public static final String UNKNOWN_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown, supported values are: jar, war, zip, ftp, auto and none.";

	@Override
	public RuntimeHandler getRuntimeHandler(final WebAppConfiguration config, final Azure azureClient)
			throws AzureExecutionException {
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
			throw new AzureExecutionException("Unknown ");
		}
		return builder.appName(config.getAppName()).resourceGroup(config.getResourceGroup()).region(config.getRegion())
				.pricingTier(config.getPricingTier()).servicePlanName(config.getServicePlanName())
				.servicePlanResourceGroup((config.getServicePlanResourceGroup())).azure(azureClient).build();
	}

	protected WebAppRuntimeHandler.Builder getDockerRuntimeHandlerBuilder(final WebAppConfiguration config)
			throws AzureExecutionException {

		final WebAppRuntimeHandler.Builder<? extends WebAppRuntimeHandler.Builder> builder;

		final IDockerCredentialProvider dockerCredentialProvider = StringUtils.isNotBlank(config.getServerId())
				? new MavenDockerCredentialProvider(config.getMavenSettings(), config.getServerId())
				: null;
		final DockerImageType imageType = AppServiceUtils.getDockerImageType(config.getImage(),
				StringUtils.isNotBlank(config.getServerId()), config.getRegistryUrl());
		switch (imageType) {
		case PUBLIC_DOCKER_HUB:
			builder = new PublicDockerHubRuntimeHandlerImpl.Builder();
			break;
		case PRIVATE_DOCKER_HUB:
			builder = new PrivateDockerHubRuntimeHandlerImpl.Builder();
			break;
		case PRIVATE_REGISTRY:
			builder = new PrivateRegistryRuntimeHandlerImpl.Builder();
			break;
		default:
			throw new AzureExecutionException("Invalid docker runtime configured.");
		}
		builder.image(config.getImage()).dockerCredentialProvider(dockerCredentialProvider).registryUrl(config.getRegistryUrl());
		return builder;
	}

	@Override
	public SettingsHandler getSettingsHandler(final AbstractWebAppMojo mojo) {
		return new SettingsHandlerImpl(mojo);
	}

	@Override
	public ArtifactHandler getArtifactHandler(final AbstractWebAppMojo mojo) throws AzureExecutionException {
		switch (SchemaVersion.fromString(mojo.getSchemaVersion())) {
		case V1:
			throw new AzureExecutionException(SchemaVersion.V1_SCHEMA_DEPRECATED);
		case V2:
			return getV2ArtifactHandler(mojo);
		default:
			throw new AzureExecutionException(SchemaVersion.UNKNOWN_SCHEMA_VERSION);
		}
	}

	protected ArtifactHandler getV2ArtifactHandler(AbstractWebAppMojo mojo) {
		return new ArtifactHandlerImplV2.Builder()
				.stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
				.runtime(mojo.getRuntime()).build();
	}

	@Override
	public DeploymentSlotHandler getDeploymentSlotHandler(AbstractWebAppMojo mojo) {
		return new DeploymentSlotHandler(mojo);
	}
}
