/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.function.AzureStorageHelper;
import com.microsoft.azure.toolkit.lib.legacy.function.Constants;

import javax.annotation.Nonnull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.Date;

public class MSFunctionDeployHandler implements IFunctionDeployHandler {
    private static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-deployment-packages";
    private static final String UPLOAD_PACKAGE_START = "Step 1 of 3: Uploading ZIP file to Azure Storage...";
    private static final String UPLOAD_PACKAGE_DONE = "Successfully uploaded ZIP file to ";
    private static final String DEPLOY_PACKAGE_START = "Step 2 of 3: Deploying package to Azure Function App...";
    private static final String DEPLOY_PACKAGE_DONE = "Successfully deployed to Azure Function App.";
    private static final String DELETE_PACKAGE_START = "Step 3 of 3: Deleting deployment package from Azure Storage...";
    private static final String DELETE_PACKAGE_DONE = "Successfully deleted deployment package ";
    private static final String DELETE_PACKAGE_FAIL = "Failed to delete deployment package ";

    @Override
    public void deploy(@Nonnull final File file, @Nonnull final WebAppBase webAppBase) {
        final BlobServiceClient storageAccount = DeployUtils.getBlobServiceClient(webAppBase);

        final String blobName = getBlobName(webAppBase);

        final String packageUri = uploadPackageToAzureStorage(file, storageAccount, blobName);

        deployWithPackageUri(webAppBase, packageUri, () -> deletePackageFromAzureStorage(storageAccount, blobName));
    }

    private String getBlobName(final WebAppBase functionApp) {
        return functionApp.name()
                .concat(new SimpleDateFormat(".yyyyMMddHHmmssSSS").format(new Date()))
                .concat(Constants.ZIP_EXT);
    }

    private String uploadPackageToAzureStorage(final File zipPackage, final BlobServiceClient storageAccount, final String blobName) {
        AzureMessager.getMessager().info(UPLOAD_PACKAGE_START);
        final BlobClient blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount, DEPLOYMENT_PACKAGE_CONTAINER, blobName);
        final String packageUri = AzureStorageHelper.getSASToken(blob, Period.ofDays(1)); // no need for a long period as it will be deleted after deployment
        AzureMessager.getMessager().info(UPLOAD_PACKAGE_DONE + blob.getBlobUrl());
        return packageUri;
    }

    private void deployWithPackageUri(final WebAppBase target, final String packageUri, Runnable onDeployFinish) {
        try {
            AzureMessager.getMessager().info(DEPLOY_PACKAGE_START);
            target.deploy().withPackageUri(packageUri).execute().complete();
            AzureMessager.getMessager().info(DEPLOY_PACKAGE_DONE);
        } finally {
            onDeployFinish.run();
        }
    }

    private void deletePackageFromAzureStorage(final BlobServiceClient storageAccount, final String blobName) {
        try {
            AzureMessager.getMessager().info(DELETE_PACKAGE_START);
            AzureStorageHelper.deleteBlob(storageAccount, DEPLOYMENT_PACKAGE_CONTAINER, blobName);
            AzureMessager.getMessager().info(DELETE_PACKAGE_DONE + blobName);
        } catch (Exception e) {
            AzureMessager.getMessager().error(DELETE_PACKAGE_FAIL + blobName);
        }
    }
}
