package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

public class NONEArtifactHandlerImpl implements ArtifactHandler {
    @Override
    public void publish(DeployTarget deployTarget) {
        // does nothing
    }
}
