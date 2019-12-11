/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.artifact;

import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.AzureStorageHelper;
import com.microsoft.azure.maven.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.time.Period;

import static com.microsoft.azure.maven.function.Constants.APP_SETTING_WEBSITE_RUN_FROM_PACKAGE;

/**
 * RunFromBlobArtifactHandlerImpl
 */
public class RunFromBlobArtifactHandlerImpl extends ArtifactHandlerBase {

    public static final int SAS_EXPIRE_DATE_BY_YEAR = 10;
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
        final File zipPackage = FunctionArtifactHelper.createFunctionArtifact(stagingDirectoryPath);
        final CloudStorageAccount storageAccount = FunctionArtifactHelper.getCloudStorageAccount(deployTarget);
        final CloudBlockBlob blob = deployArtifactToAzureStorage(deployTarget, zipPackage, storageAccount);
        final String sasToken = AzureStorageHelper.getSASToken(blob, Period.ofYears(SAS_EXPIRE_DATE_BY_YEAR));
        FunctionArtifactHelper.updateAppSetting(deployTarget, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, sasToken);
    }

    private CloudBlockBlob deployArtifactToAzureStorage(DeployTarget deployTarget, File zipPackage, CloudStorageAccount storageAccount) throws Exception {
        log.info(String.format(DEPLOY_START, deployTarget.getName()));
        final CloudBlockBlob blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                DEPLOYMENT_PACKAGE_CONTAINER, zipPackage.getName());
        final String blobUri = blob.getUri().getHost() + blob.getUri().getPath();
        log.info(String.format(DEPLOY_FINISH, blobUri));
        return blob;
    }
}
