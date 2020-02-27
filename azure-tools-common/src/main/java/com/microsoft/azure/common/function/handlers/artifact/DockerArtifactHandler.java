/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function.handlers.artifact;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.common.logging.Log;

public class DockerArtifactHandler extends ArtifactHandlerBase {

    public static final String SKIP_DOCKER_DEPLOYMENT = "Skip deployment for docker functions";

    public static class Builder extends ArtifactHandlerBase.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ArtifactHandlerBase build() {
            return new DockerArtifactHandler(self());
        }
    }

    protected DockerArtifactHandler(Builder builder) {
        super(builder);
    }

    @Override
    public void publish(DeployTarget deployTarget) {
        Log.prompt(SKIP_DOCKER_DEPLOYMENT);
        deployTarget.getApp().restart();
    }
}
