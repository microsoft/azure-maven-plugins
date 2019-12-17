/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.artifact;

import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.maven.handlers.ArtifactHandler;


public abstract class ArtifactHandlerBase implements ArtifactHandler {
    protected static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    protected static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    protected static final String DEPLOY_ABORT = "Deployment is aborted.";
    protected static final String NO_RESOURCES_CONFIG = "<resources> is empty. " +
            "Please make sure it is configured in pom.xml.";

    protected IProject project;

    protected String stagingDirectoryPath;

    public abstract static class Builder<T extends Builder<T>> {
        protected IProject project;

        protected String stagingDirectoryPath;

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
    }

    protected ArtifactHandlerBase(Builder<?> builder) {
        this.project = builder.project;
        this.stagingDirectoryPath = builder.stagingDirectoryPath;
    }
}
