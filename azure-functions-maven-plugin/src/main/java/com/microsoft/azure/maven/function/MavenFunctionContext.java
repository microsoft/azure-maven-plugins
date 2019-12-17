package com.microsoft.azure.maven.function;

import java.util.Map;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.function.IFunctionContext;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;

public class MavenFunctionContext implements IFunctionContext {
	private AbstractFunctionMojo mojo;
	private IProject project;
	public MavenFunctionContext(AbstractFunctionMojo mojo) {
		this.mojo = mojo;
		this.project = MavenProjectConvert.getProject(mojo.getProject());
	}
	@Override
	public String getDeploymentStagingDirectoryPath() {
		return mojo.getDeploymentStagingDirectoryPath();
	}
	@Override
	public String getSubscription() {
		return mojo.getSubscriptionId();
	}
	@Override
	public String getAppName() {
		return mojo.getAppName();
	}
	@Override
	public String getResourceGroup() {
		return mojo.getResourceGroup();
	}
	@Override
	public RuntimeConfiguration getRuntime() {
		return mojo.getRuntime();
	}
	@Override
	public String getRegion() {
		return mojo.getRegion();
	}
	@Override
	public String getPricingTier() {
		return mojo.getPricingTier();
	}
	@Override
	public String getAppServicePlanResourceGroup() {
		return mojo.getAppServicePlanResourceGroup();
	}
	@Override
	public String getAppServicePlanName() {
		return mojo.getAppServicePlanName();
	}
	@Override
	public Map getAppSettings() {
		return mojo.getAppSettings();
	}
	@Override
	public AuthConfiguration getAuth() {
		return null;
	}
	@Override
	public String getDeploymentType() {
		return mojo.getDeploymentType();
	}
	@Override
	public IDockerCredentialProvider getDockerCredentialProvider() {
		return getProvider(IDockerCredentialProvider.class);
	}
	private <T>T getProvider(Class<T> class1) {
		return null;
	}
	@Override
	public IProject getProject() {
		return project;
	}



}
