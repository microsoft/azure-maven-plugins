package com.microsoft.azure.common.function;

import java.util.Map;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;

public interface IFunctionContext {
	String getDeploymentStagingDirectoryPath();

	String getProjectDir();

	String getSubscription();

	String getAppName();

	String getResourceGroup();

	RuntimeConfiguration getRuntime();

	String getRegion();

	String getPricingTier();

	String getAppServicePlanResourceGroup();

	String getAppServicePlanName();

	Map getAppSettings();

	AuthConfiguration getAuth();

	String getDeploymentType();

	IDockerCredentialProvider getDockerCredentialProvider();

	IProject getProject();
}
