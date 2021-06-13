/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;

@Slf4j
public class ZIPArtifactHandlerImpl extends ArtifactHandlerBase {
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";

    public static class Builder extends ArtifactHandlerBase.Builder<Builder> {
        @Override
        protected Builder self() {
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

    @Override
    public void publish(DeployTarget target) throws AzureExecutionException {
        assureStagingDirectoryNotEmpty();

        final File zipFile = getZipFile();
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(String.format(DEPLOY_START, target.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        int retryCount = 0;
        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount += 1;
            try {
                target.zipDeploy(zipFile);
                messager.success(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
                return;
            } catch (Exception e) {
                log.debug(
                    String.format("Exception occurred when deploying the zip package: %s, " +
                        "retrying immediately (%d/%d)", e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        throw new AzureExecutionException(String.format("The zip deploy failed after %d times of retry.", retryCount));
    }

    protected File getZipFile() {
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        final File stagingDirectory = new File(stagingDirectoryPath);

        ZipUtil.pack(stagingDirectory, zipFile);
        ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
        return zipFile;
    }
}
