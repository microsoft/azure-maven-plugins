/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class ZIPArtifactHandlerImpl<T extends AbstractAppServiceMojo> extends ArtifactHandlerBase<T> {
    public ZIPArtifactHandlerImpl(@Nonnull final T mojo) {
        super(mojo);
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        prepareResources();
        assureStagingDirectoryNotEmpty();

        target.zipDeploy(getZipFile());
    }

    protected File getZipFile() {
        final String stagingDirectoryPath = getDeploymentStagingDirectoryPath();
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        final File stagingDirectory = new File(stagingDirectoryPath);

        ZipUtil.pack(stagingDirectory, zipFile);
        return zipFile;
    }
}
