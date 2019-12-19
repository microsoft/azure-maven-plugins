package com.microsoft.azure.common;

import java.util.Map;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;

public interface IAppServiceContext {
	String getDeploymentStagingDirectoryPath();

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

	Azure getAzureClient() throws AzureExecutionException;

	IProject getProject();
}
