package com.microsoft.azure.maven.function;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.function.IFunctionContext;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.common.project.JavaProject;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;

public class MavenFunctionContext implements IFunctionContext {
	private AbstractFunctionMojo mojo;
	private IProject project;
	private Map<Class<? extends Object>, Object> providerMap = new HashMap<>();

	public MavenFunctionContext(AbstractFunctionMojo mojo) {
		this.mojo = mojo;
		this.project = convertProject(mojo.getProject());
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

	public IDockerCredentialProvider getDockerCredentialProvider() {
		return getProvider(IDockerCredentialProvider.class);
	}

	@Override
	public void registerProvider(Class<? extends Object> clazz, Object provider) {
		if (clazz == null) {
			throw new IllegalArgumentException("Null provider class is illegal.");
		}

		if (provider == null) {
			throw new IllegalArgumentException("Null provider is illegal.");
		}

		if (providerMap.containsKey(clazz)) {
			throw new IllegalArgumentException(String.format("%s has already been registered.", clazz.getName()));
		}
		if (!clazz.isInterface()) {
			throw new IllegalArgumentException("The provider class should be an interface");
		}

		providerMap.put(clazz, provider);
	}


	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public <T> T getProvider(Class<T> clazz) {
		if (!providerMap.containsKey(clazz)) {
			throw new IllegalArgumentException(String.format("%s has not been registered.", clazz.getName()));
		}
		return (T) providerMap.get(clazz);
	}


	private static IProject convertProject(MavenProject project) {
		final JavaProject proj = new JavaProject();
		proj.setProjectName(project.getName());
		proj.setBaseDirectory(project.getBasedir().toPath());
		String jarArtifactName = project.getBuild().getFinalName() + "." + project.getPackaging();
		proj.setJarArtifact(Paths.get(project.getBuild().getDirectory(), jarArtifactName));
		proj.setClassesOutputDirectory(Paths.get(project.getBuild().getOutputDirectory()));
		proj.setDependencies(collectDependencyPaths(project.getArtifacts()));
		proj.setBuildDirectory(Paths.get(project.getBuild().getDirectory()));
		return proj;
	}

	private static List<Path> collectDependencyPaths(Set<Artifact> dependencies) {
		List<Path> results =  new ArrayList<>();
		for (Artifact artifact : dependencies) {
			results.add(artifact.getFile().toPath());
		}
		return results;
	}
}
