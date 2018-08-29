/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Utility class
 */
public final class Utils {
    /**
     * Get server credential from Maven settings by server Id.
     *
     * @param settings Maven settings object.
     * @param serverId Server Id.
     * @return Server object if it exists in settings. Otherwise return null.
     */
    public static Server getServer(final Settings settings, final String serverId) {
        if (settings == null || StringUtils.isEmpty(serverId)) {
            return null;
        }
        return settings.getServer(serverId);
    }

    /**
     * Assure the server exists.
     * @param server
     * @param serverId
     * @throws MojoExecutionException
     */
    public static void assureServerExist(final Server server, final String serverId) throws MojoExecutionException {
        if (server == null) {
            throw new MojoExecutionException(String.format("Server not found in settings.xml. ServerId=%s", serverId));
        }
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param server Server object.
     * @param key    Key string.
     * @return String value if key exists; otherwise, return null.
     */
    public static String getValueFromServerConfiguration(final Server server, final String key) {
        if (server == null) {
            return null;
        }

        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        final Xpp3Dom node = configuration.getChild(key);
        if (node == null) {
            return null;
        }

        return node.getValue();
    }

    /**
     * Copy resources to target directory using Maven resource filtering so that we don't have to handle
     * recursive directory listing and pattern matching.
     * In order to disable filtering, the "filtering" property is force set to False.
     *
     * @param project
     * @param session
     * @param filtering
     * @param resources
     * @param targetDirectory
     * @throws IOException
     */
    public static void copyResources(final MavenProject project, final MavenSession session,
                                     final MavenResourcesFiltering filtering, final List<Resource> resources,
                                     final String targetDirectory) throws IOException {
        for (final Resource resource : resources) {
            final String targetPath = resource.getTargetPath() == null ? "" : resource.getTargetPath();
            resource.setTargetPath(Paths.get(targetDirectory, targetPath).toString());
            resource.setFiltering(false);
        }

        final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
                resources,
                new File(targetDirectory),
                project,
                "UTF-8",
                null,
                Collections.EMPTY_LIST,
                session
        );

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
