/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers.artifact;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.function.AzureStorageHelper;
import com.microsoft.azure.common.function.Constants;
import com.microsoft.azure.common.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import javax.annotation.Nonnull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.Date;

public class MSDeployArtifactHandlerImpl extends ArtifactHandlerBase {
    public static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-deployment-packages";
    public static final String CREATE_ZIP_START = "Step 1 of 4: Creating ZIP file...";
    public static final String CREATE_ZIP_DONE = "Successfully saved ZIP file at ";
    public static final String UPLOAD_PACKAGE_START = "Step 2 of 4: Uploading ZIP file to Azure Storage...";
    public static final String UPLOAD_PACKAGE_DONE = "Successfully uploaded ZIP file to ";
    public static final String DEPLOY_PACKAGE_START = "Step 3 of 4: Deploying Azure Function App with package...";
    public static final String DEPLOY_PACKAGE_DONE = "Successfully deployed Azure Function App.";
    public static final String DELETE_PACKAGE_START = "Step 4 of 4: Deleting deployment package from Azure Storage...";
    public static final String DELETE_PACKAGE_DONE = "Successfully deleted deployment package ";
    public static final String DELETE_PACKAGE_FAIL = "Failed to delete deployment package ";

    protected final String functionAppName;

    public static class Builder extends ArtifactHandlerBase.Builder<Builder> {
        private String functionAppName;

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public MSDeployArtifactHandlerImpl build() {
            return new MSDeployArtifactHandlerImpl(this);
        }

        public Builder functionAppName(final String value) {
            this.functionAppName = value;
            return self();
        }
    }

    private MSDeployArtifactHandlerImpl(@Nonnull final Builder builder) {
        super(builder);
        this.functionAppName = builder.functionAppName;
    }

    @Override
    public void publish(final DeployTarget target) throws AzureExecutionException {
        final File zipPackage = createZipPackage();

        final CloudStorageAccount storageAccount = FunctionArtifactHelper.getCloudStorageAccount(target);

        final String blobName = getBlobName();

        final String packageUri = uploadPackageToAzureStorage(zipPackage, storageAccount, blobName);

        deployWithPackageUri(target, packageUri, () -> deletePackageFromAzureStorage(storageAccount, blobName));
    }

    protected File createZipPackage() throws AzureExecutionException {
        Log.prompt("");
        Log.prompt(CREATE_ZIP_START);
        final File zipPackage = FunctionArtifactHelper.createFunctionArtifact(stagingDirectoryPath);
        Log.prompt(CREATE_ZIP_DONE + stagingDirectoryPath.concat(Constants.ZIP_EXT));
        return zipPackage;
    }

    protected String getBlobName() {
        return functionAppName
                .concat(new SimpleDateFormat(".yyyyMMddHHmmssSSS").format(new Date()))
                .concat(Constants.ZIP_EXT);
    }

    protected String uploadPackageToAzureStorage(final File zipPackage, final CloudStorageAccount storageAccount,
                                                 final String blobName) throws AzureExecutionException {
        Log.prompt(UPLOAD_PACKAGE_START);
        final CloudBlockBlob blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                DEPLOYMENT_PACKAGE_CONTAINER, blobName, BlobContainerPublicAccessType.OFF);
        final String packageUri = AzureStorageHelper.getSASToken(blob, Period.ofDays(1)); // no need for a long period as it will be deleted after deployment
        Log.prompt(UPLOAD_PACKAGE_DONE + blob.getUri().toString());
        return packageUri;
    }

    protected void deployWithPackageUri(final DeployTarget target, final String packageUri, Runnable onDeployFinish) {
        try {
            Log.prompt(DEPLOY_PACKAGE_START);
            target.msDeploy(packageUri, false);
            Log.prompt(DEPLOY_PACKAGE_DONE);
        } finally {
            onDeployFinish.run();
        }
    }

    protected void deletePackageFromAzureStorage(final CloudStorageAccount storageAccount, final String blobName) {
        try {
            Log.prompt(DELETE_PACKAGE_START);
            AzureStorageHelper.deleteBlob(storageAccount, DEPLOYMENT_PACKAGE_CONTAINER, blobName);
            Log.prompt(DELETE_PACKAGE_DONE + blobName);
        } catch (Exception e) {
            Log.error(DELETE_PACKAGE_FAIL + blobName);
        }
    }
}
