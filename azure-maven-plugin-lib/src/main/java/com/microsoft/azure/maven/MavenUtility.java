package com.microsoft.azure.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.deploytarget.DeployTarget;


public class MavenUtility {
	protected static final String STAGING_FOLDER_EMPTY = "Staging directory: '%s' is empty, please check "
			+ "your <resources> configuration.(Have you executed mvn package before this command?)";

	protected static final String NO_RESOURCES_CONFIG = "<resources> is empty. "
			+ "Please make sure it is configured in pom.xml.";

	public static void assureStagingDirectoryNotEmpty(String stagingDirectoryPath) throws AzureExecutionException {
		final File stagingDirectory = new File(stagingDirectoryPath);
		final File[] files = stagingDirectory.listFiles();
		if (!stagingDirectory.exists() || !stagingDirectory.isDirectory() || files == null || files.length == 0) {
			throw new AzureExecutionException(String.format(STAGING_FOLDER_EMPTY, stagingDirectory.getAbsolutePath()));
		}
	}


	public static boolean isResourcesPreparationRequired(final DeployTarget target) {
        return target.getApp() instanceof WebApp || target.getApp() instanceof DeploymentSlot;
    }

	/**
	 * Copy resources to target directory using Maven resource filtering so that we
	 * don't have to handle recursive directory listing and pattern matching. In
	 * order to disable filtering, the "filtering" property is force set to False.
	 *
	 * @param project
	 * @param session
	 * @param filtering
	 * @param resources
	 * @param targetDirectory
	 * @throws IOException
	 */
	public static void prepareResources(final MavenProject project, final MavenSession session,
			final MavenResourcesFiltering filtering, final List<Resource> resources, final String stagingDirectoryPath)
			throws IOException, AzureExecutionException {
		if (resources == null || resources.isEmpty()) {
			throw new AzureExecutionException(NO_RESOURCES_CONFIG);
		}

		copyResources(project, session, filtering, resources, stagingDirectoryPath);
	}


	private static void copyResources(final MavenProject project, final MavenSession session,
			final MavenResourcesFiltering filtering, final List<Resource> resources, final String targetDirectory)
			throws IOException {
		for (final Resource resource : resources) {
			final String targetPath = resource.getTargetPath() == null ? "" : resource.getTargetPath();
			resource.setTargetPath(Paths.get(targetDirectory, targetPath).toString());
			resource.setFiltering(false);
		}

		final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resources,
				new File(targetDirectory), project, "UTF-8", null, Collections.EMPTY_LIST, session);

		// Configure executor
		mavenResourcesExecution.setEscapeWindowsPaths(true);
		mavenResourcesExecution.setInjectProjectBuildFilters(false);
		mavenResourcesExecution.setOverwrite(true);
		mavenResourcesExecution.setIncludeEmptyDirs(false);
		mavenResourcesExecution.setSupportMultiLineFiltering(false);

		// Filter resources
		try {
			filtering.filterResources(mavenResourcesExecution);
		} catch (MavenFilteringException ex) {
			throw new IOException("Failed to copy resources", ex);
		}
	}
}
