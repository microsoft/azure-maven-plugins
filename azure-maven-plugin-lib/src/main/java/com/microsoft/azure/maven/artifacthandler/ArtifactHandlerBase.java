/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.Utils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class ArtifactHandlerBase<T extends AbstractAppServiceMojo> implements ArtifactHandler {
    protected T mojo;

    public ArtifactHandlerBase(@Nonnull final T mojo) {
        this.mojo = mojo;
    }

    protected void assureStagingDirectoryNotEmpty() throws MojoExecutionException {
        final File stagingDirectory = new File(mojo.getDeploymentStagingDirectoryPath());
        final File[] files = stagingDirectory.listFiles();
        if (!stagingDirectory.exists() || !stagingDirectory.isDirectory() || files == null || files.length == 0) {
            throw new MojoExecutionException(String.format("Staging directory: '%s' is empty.",
                    stagingDirectory.getAbsolutePath()));
        }
    }

    protected void prepareResources() throws IOException {
        final List<Resource> resources = this.mojo.getResources();

        if (resources != null && !resources.isEmpty()) {
            Utils.copyResources(mojo.getProject(), mojo.getSession(),
                    mojo.getMavenResourcesFiltering(), resources, mojo.getDeploymentStagingDirectoryPath());
        }
    }
}
