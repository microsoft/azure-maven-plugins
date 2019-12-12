/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.handlers.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class ArtifactHandlerBase implements ArtifactHandler {
    protected static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    protected static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    protected static final String DEPLOY_ABORT = "Deployment is aborted.";
    protected static final String NO_RESOURCES_CONFIG = "<resources> is empty. " +
            "Please make sure it is configured in pom.xml.";
    protected static final String STAGING_FOLDER_EMPTY = "Staging directory: '%s' is empty, please check " +
            "your <resources> configuration.(Have you executed mvn package before this command?)";
    protected MavenProject project;
    protected MavenSession session;
    protected MavenResourcesFiltering filtering;
    protected List<Resource> resources;
    protected String stagingDirectoryPath;
    protected String buildDirectoryAbsolutePath;
    protected Log log;

    public abstract static class Builder<T extends Builder<T>> {
        private MavenProject project;
        private MavenSession session;
        private MavenResourcesFiltering filtering;
        private List<Resource> resources;
        private String stagingDirectoryPath;
        private String buildDirectoryAbsolutePath;
        private Log log;

        protected abstract T self();

        public abstract ArtifactHandlerBase build();

        public T project(final MavenProject value) {
            this.project = value;
            return self();
        }

        public T session(final MavenSession value) {
            this.session = value;
            return self();
        }

        public T filtering(final MavenResourcesFiltering value) {
            this.filtering = value;
            return self();
        }

        public T resources(final List<Resource> value) {
            this.resources = value;
            return self();
        }

        public T stagingDirectoryPath(final String value) {
            this.stagingDirectoryPath = value;
            return self();
        }

        public T buildDirectoryAbsolutePath(final String value) {
            this.buildDirectoryAbsolutePath = value;
            return self();
        }

        public T log(final Log value) {
            this.log = value;
            return self();
        }
    }

    protected ArtifactHandlerBase(Builder<?> builder) {
        this.project = builder.project;
        this.session = builder.session;
        this.filtering = builder.filtering;
        this.resources = builder.resources;
        this.stagingDirectoryPath = builder.stagingDirectoryPath;
        this.buildDirectoryAbsolutePath = builder.buildDirectoryAbsolutePath;
        this.log = builder.log;
    }

    protected void assureStagingDirectoryNotEmpty() throws AzureExecutionException {
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File[] files = stagingDirectory.listFiles();
        if (!stagingDirectory.exists() || !stagingDirectory.isDirectory() || files == null || files.length == 0) {
            throw new AzureExecutionException(String.format(STAGING_FOLDER_EMPTY, stagingDirectory.getAbsolutePath()));
        }
    }

    protected void prepareResources() throws IOException, AzureExecutionException {
        if (resources == null || resources.isEmpty()) {
            throw new AzureExecutionException(NO_RESOURCES_CONFIG);
        }

        Utils.copyResources(project, session, filtering, resources, stagingDirectoryPath);
    }
}
