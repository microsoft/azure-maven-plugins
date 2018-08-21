/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class ZIPArtifactHandlerImpl<T extends AbstractAppServiceMojo> extends ArtifactHandlerBase<T> {
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";

    public ZIPArtifactHandlerImpl(@Nonnull final T mojo) {
        super(mojo);
    }

    protected boolean isResourcesPreparationRequired(final DeployTarget target) throws MojoExecutionException {
        return (target.getApp() instanceof WebApp || target.getApp() instanceof DeploymentSlot) &&
            DeploymentType.JAR != mojo.getDeploymentType();
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        if (isResourcesPreparationRequired(target)) {
            prepareResources();
        }
        assureStagingDirectoryNotEmpty();

        final File zipFile = getZipFile();

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
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
        ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
        return zipFile;
    }
}
