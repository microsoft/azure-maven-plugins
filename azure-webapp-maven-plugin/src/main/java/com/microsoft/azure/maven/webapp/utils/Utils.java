/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    private static final String CREATE_TEMP_FILE_FAIL = "Failed to create temp file %s.%s";

    private static final String NO_RESOURCES_CONFIG = "<resources> is empty. Please make sure it is configured in pom.xml.";

    public static File createTempFile(final String prefix, final String suffix) throws AzureExecutionException {
        try {
            final File zipFile = File.createTempFile(prefix, suffix);
            zipFile.deleteOnExit();
            return zipFile;
        } catch (IOException e) {
            throw new AzureExecutionException(String.format(CREATE_TEMP_FILE_FAIL, prefix, suffix), e.getCause());
        }
    }

    // Todo: Move this method to common for duplicated with Utils in spring maven plugin
    public static List<File> getArtifacts(Resource resource) {
        final List<File> result = new ArrayList<>();
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            directoryScanner.setBasedir(resource.getDirectory());
            directoryScanner.setIncludes(resource.getIncludes().toArray(new String[0]));
            final String[] exclude = resource.getExcludes() == null ? new String[0] :
                    resource.getExcludes().toArray(new String[0]);
            directoryScanner.setExcludes(exclude);
            directoryScanner.scan();
            final List<File> resourceFiles = Arrays.stream(directoryScanner.getIncludedFiles())
                    .map(path -> new File(resource.getDirectory(), path))
                    .collect(Collectors.toList());
            result.addAll(resourceFiles);
        }
        return result;
    }

    public static void prepareResources(final MavenProject project, final MavenSession session,
            final MavenResourcesFiltering filtering, final List<Resource> resources, final String stagingDirectoryPath)
            throws IOException, AzureExecutionException {
        if (resources == null || resources.isEmpty()) {
            throw new AzureExecutionException(NO_RESOURCES_CONFIG);
        }

        copyResources(project, session, filtering, resources, stagingDirectoryPath);
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

    private Utils() {

    }
}
