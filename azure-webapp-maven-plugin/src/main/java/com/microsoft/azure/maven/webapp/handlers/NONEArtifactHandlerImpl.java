/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

public class NONEArtifactHandlerImpl implements ArtifactHandler {
    private AbstractWebAppMojo mojo;

    public NONEArtifactHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish(DeployTarget deployTarget) {
        this.mojo.getLog().info("Detect <deploymentType> was set to NONE, skip deployment.");
    }
}
