/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.artifact;

import static com.microsoft.azure.maven.function.Constants.APP_SETTING_WEBSITE_RUN_FROM_PACKAGE;

import java.io.File;
import java.time.Period;

import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.AzureStorageHelper;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * RunFromBlobArtifactHandlerImpl
 */
public class RunFromBlobArtifactHandlerImpl extends ArtifactHandlerBase {

    public static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-run-from-packages";

    public static class Builder extends ArtifactHandlerBase.Builder<RunFromBlobArtifactHandlerImpl.Builder> {
        @Override
        protected RunFromBlobArtifactHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public RunFromBlobArtifactHandlerImpl build() {
            return new RunFromBlobArtifactHandlerImpl(this);
        }
    }

    protected RunFromBlobArtifactHandlerImpl(RunFromBlobArtifactHandlerImpl.Builder builder) {
        super(builder);
    }

    @Override
    public void publish(DeployTarget deployTarget) throws Exception {
        final File zipPackage = FunctionArtifactHelper.createZipPackage(stagingDirectoryPath, log);
        final CloudStorageAccount storageAccount = FunctionArtifactHelper.getCloudStorageAccount(deployTarget, log);
        final CloudBlockBlob blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                DEPLOYMENT_PACKAGE_CONTAINER, zipPackage.getName());
        final String sasToken = AzureStorageHelper.getSASToken(blob, Period.ofYears(10));
        FunctionArtifactHelper.updateAppSetting(deployTarget, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, sasToken);
    }
}