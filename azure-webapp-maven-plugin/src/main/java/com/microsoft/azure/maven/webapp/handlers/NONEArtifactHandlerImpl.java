/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

public class NONEArtifactHandlerImpl extends ArtifactHandlerBase {

    public static class Builder extends ArtifactHandlerBase.Builder<NONEArtifactHandlerImpl.Builder> {
        @Override
        protected NONEArtifactHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public NONEArtifactHandlerImpl build() {
            return new NONEArtifactHandlerImpl(this);
        }
    }

    protected NONEArtifactHandlerImpl(Builder builder) {
        super(builder);
    }

    @Override
    public void publish(DeployTarget deployTarget) {
        log.info("The value of <deploymentType> is NONE, skip deployment.");
    }
}
