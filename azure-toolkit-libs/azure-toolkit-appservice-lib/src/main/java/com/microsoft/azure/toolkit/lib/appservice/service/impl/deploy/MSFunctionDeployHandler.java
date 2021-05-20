/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.function.AzureStorageHelper;
import com.microsoft.azure.toolkit.lib.legacy.function.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.Date;

public class MSFunctionDeployHandler implements IFunctionDeployHandler {
    private static final ClientLogger LOGGER = new ClientLogger(MSFunctionDeployHandler.class);
    private static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-deployment-packages";
    private static final String UPLOAD_PACKAGE_START = "Step 1 of 3: Uploading ZIP file to Azure Storage...";
    private static final String UPLOAD_PACKAGE_DONE = "Successfully uploaded ZIP file to ";
    private static final String DEPLOY_PACKAGE_START = "Step 2 of 3: Deploying Azure Function App with package...";
    private static final String DEPLOY_PACKAGE_DONE = "Successfully deployed Azure Function App.";
    private static final String DELETE_PACKAGE_START = "Step 3 of 3: Deleting deployment package from Azure Storage...";
    private static final String DELETE_PACKAGE_DONE = "Successfully deleted deployment package ";
    private static final String DELETE_PACKAGE_FAIL = "Failed to delete deployment package ";

    @Override
    public void deploy(final File file, final WebAppBase webAppBase) {
        final CloudStorageAccount storageAccount = DeployUtils.getCloudStorageAccount(webAppBase);

        final String blobName = getBlobName(webAppBase);

        final String packageUri = uploadPackageToAzureStorage(file, storageAccount, blobName);

        deployWithPackageUri(webAppBase, packageUri, () -> deletePackageFromAzureStorage(storageAccount, blobName));
    }

    protected String getBlobName(final WebAppBase functionApp) {
        return functionApp.name()
                .concat(new SimpleDateFormat(".yyyyMMddHHmmssSSS").format(new Date()))
                .concat(Constants.ZIP_EXT);
    }

    protected String uploadPackageToAzureStorage(final File zipPackage, final CloudStorageAccount storageAccount,
                                                 final String blobName) {
        LOGGER.info(UPLOAD_PACKAGE_START);
        try {
            final CloudBlockBlob blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                    DEPLOYMENT_PACKAGE_CONTAINER, blobName, BlobContainerPublicAccessType.OFF);
            final String packageUri = AzureStorageHelper.getSASToken(blob, Period.ofDays(1)); // no need for a long period as it will be deleted after deployment
            LOGGER.info(UPLOAD_PACKAGE_DONE + blob.getUri().toString());
            return packageUri;
        } catch (AzureExecutionException e) {
            throw new AzureToolkitRuntimeException("Failed to upload package to azure storage", e);
        }
    }

    protected void deployWithPackageUri(final WebAppBase target, final String packageUri, Runnable onDeployFinish) {
        try {
            LOGGER.info(DEPLOY_PACKAGE_START);
            target.deploy().withPackageUri(packageUri).execute().complete();
            LOGGER.info(DEPLOY_PACKAGE_DONE);
        } finally {
            onDeployFinish.run();
        }
    }

    protected void deletePackageFromAzureStorage(final CloudStorageAccount storageAccount, final String blobName) {
        try {
            LOGGER.info(DELETE_PACKAGE_START);
            AzureStorageHelper.deleteBlob(storageAccount, DEPLOYMENT_PACKAGE_CONTAINER, blobName);
            LOGGER.info(DELETE_PACKAGE_DONE + blobName);
        } catch (Exception e) {
            LOGGER.error(DELETE_PACKAGE_FAIL + blobName);
        }
    }
}
