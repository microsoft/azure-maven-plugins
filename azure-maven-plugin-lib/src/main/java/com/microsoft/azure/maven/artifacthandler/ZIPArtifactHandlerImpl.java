/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class ZIPArtifactHandlerImpl<T extends AbstractAppServiceMojo> extends ArtifactHandlerBase<T> {
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;

    public ZIPArtifactHandlerImpl(@Nonnull final T mojo) {
        super(mojo);
    }

    protected boolean isResourcesPreparationRequired(final DeployTarget target) {
        return !(target.getApp() instanceof FunctionApp ||
            StringUtils.equalsIgnoreCase(mojo.getDeploymentType(), "jar"));
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        if (isResourcesPreparationRequired(target)) {
            prepareResources();
        }
        assureStagingDirectoryNotEmpty();

        final File zipFile = getZipFile();
        int retryCount = 0;
        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount += 1;
            try {
                target.zipDeploy(zipFile);
                return;
            } catch (Exception e) {
                mojo.getLog().debug(
                    String.format("Exception occurred when deploying the zip package: %s, " +
                        "retrying immediately (%d/%d)", e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        throw new MojoExecutionException(String.format("The zip deploy failed after %d times of retry.", retryCount));
    }

    protected File getZipFile() {
        final String stagingDirectoryPath = mojo.getDeploymentStagingDirectoryPath();
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        final File stagingDirectory = new File(stagingDirectoryPath);

        ZipUtil.pack(stagingDirectory, zipFile);
        return zipFile;
    }
}
