/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.project.IProject;

import java.io.File;

public abstract class ArtifactHandlerBase implements ArtifactHandler {
    protected static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    protected static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    protected static final String DEPLOY_ABORT = "Deployment is aborted.";
    protected static final String STAGING_FOLDER_EMPTY = "Staging directory: '%s' is empty, please check " +
            "your <resources> configuration.(Have you executed mvn package before this command?)";
    protected IProject project;
    protected String stagingDirectoryPath;
    protected String buildDirectoryAbsolutePath;

    public abstract static class Builder<T extends Builder<T>> {
        private IProject project;
        private String stagingDirectoryPath;
        private String buildDirectoryAbsolutePath;

        protected abstract T self();

        public abstract ArtifactHandlerBase build();

        public T project(final IProject value) {
            this.project = value;
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

    }

    protected ArtifactHandlerBase(Builder<?> builder) {
        this.project = builder.project;
        this.stagingDirectoryPath = builder.stagingDirectoryPath;
        this.buildDirectoryAbsolutePath = builder.buildDirectoryAbsolutePath;
    }

    protected void assureStagingDirectoryNotEmpty() throws AzureExecutionException {
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File[] files = stagingDirectory.listFiles();
        if (!stagingDirectory.exists() || !stagingDirectory.isDirectory() || files == null || files.length == 0) {
            throw new AzureExecutionException(String.format(STAGING_FOLDER_EMPTY, stagingDirectory.getAbsolutePath()));
        }
    }

}
