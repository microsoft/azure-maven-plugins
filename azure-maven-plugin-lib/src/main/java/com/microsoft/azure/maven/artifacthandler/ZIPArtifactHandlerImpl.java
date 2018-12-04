/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.zip.ZipUtil;
import java.io.File;
import java.io.IOException;

public class ZIPArtifactHandlerImpl extends ArtifactHandlerBase {
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";

    public static class Builder extends ArtifactHandlerBase.Builder<ZIPArtifactHandlerImpl.Builder> {
        @Override
        protected ZIPArtifactHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public ZIPArtifactHandlerImpl build() {
            return new ZIPArtifactHandlerImpl(this);
        }
    }

    protected ZIPArtifactHandlerImpl(final Builder builder) {
        super(builder);
    }

    /**
     * Web App and Deployment Slot need the handler to prepare resources to staging folder.
     * Function App does not.
     */
    protected boolean isResourcesPreparationRequired(final DeployTarget target) {
        return !(target.getApp() instanceof FunctionApp);
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        if (isResourcesPreparationRequired(target)) {
            prepareResources();
        }
        assureStagingDirectoryNotEmpty();

        final File zipFile = getZipFile();
        log.info(String.format(DEPLOY_START, target.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        int retryCount = 0;
        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount += 1;
            try {
                target.zipDeploy(zipFile);
                log.info(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
                return;
            } catch (Exception e) {
                log.debug(
                    String.format("Exception occurred when deploying the zip package: %s, " +
                        "retrying immediately (%d/%d)", e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        throw new MojoExecutionException(String.format("The zip deploy failed after %d times of retry.", retryCount));
    }

    protected File getZipFile() {
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        final File stagingDirectory = new File(stagingDirectoryPath);

        ZipUtil.pack(stagingDirectory, zipFile);
        ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
        return zipFile;
    }
}
