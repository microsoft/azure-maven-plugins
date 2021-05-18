/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.function.AzureStorageHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Period;

import static com.microsoft.azure.toolkit.lib.legacy.function.Constants.APP_SETTING_WEBSITE_RUN_FROM_PACKAGE;

public class RunFromBlobFunctionDeployHandler implements IFunctionDeployHandler {
    private static final int SAS_EXPIRE_DATE_BY_YEAR = 10;
    private static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-run-from-packages";
    private static final String FAILED_TO_GET_FUNCTION_APP_ARTIFACT_CONTAINER = "Failed to get Function App artifact container";
    private static final String UPDATE_ACCESS_LEVEL_TO_PRIVATE = "The blob container '%s' access level was updated to be private";

    @Override
    public void deploy(File file, WebAppBase target) {
        final CloudStorageAccount storageAccount = DeployUtils.getCloudStorageAccount(target);
        try {
            final CloudBlockBlob blob = deployArtifactToAzureStorage(target, file, storageAccount);
            final String sasToken = AzureStorageHelper.getSASToken(blob, Period.ofYears(SAS_EXPIRE_DATE_BY_YEAR));
            DeployUtils.updateFunctionAppSetting(target, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, sasToken);
        } catch (AzureExecutionException e) {
            throw new AzureToolkitRuntimeException("Failed to upload package to azure storage", e);
        }
    }

    private CloudBlockBlob deployArtifactToAzureStorage(WebAppBase deployTarget, File zipPackage, CloudStorageAccount storageAccount)
            throws AzureExecutionException {
        AzureMessager.getMessager().info(String.format(DEPLOY_START, deployTarget.name()));
        final CloudBlobContainer container = getOrCreateArtifactContainer(storageAccount);
        final String blobName = getBlobName(deployTarget, zipPackage);
        final CloudBlockBlob blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                container.getName(), blobName, BlobContainerPublicAccessType.OFF);
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, deployTarget.defaultHostname()));
        return blob;
    }

    private CloudBlobContainer getOrCreateArtifactContainer(final CloudStorageAccount storageAccount) {
        final CloudBlobClient blobContainer = storageAccount.createCloudBlobClient();
        try {
            final CloudBlobContainer container = blobContainer.getContainerReference(DEPLOYMENT_PACKAGE_CONTAINER);
            if (!container.exists()) {
                container.createIfNotExists(BlobContainerPublicAccessType.OFF, null, null);
            } else {
                updateContainerPublicAccessLevel(container);
            }
            return container;
        } catch (URISyntaxException | StorageException e) {
            throw new AzureExecutionException(FAILED_TO_GET_FUNCTION_APP_ARTIFACT_CONTAINER, e);
        }
    }

    private void updateContainerPublicAccessLevel(final CloudBlobContainer container) throws StorageException {
        final BlobContainerPermissions permissions = container.downloadPermissions();
        if (permissions.getPublicAccess() == BlobContainerPublicAccessType.OFF) {
            return;
        }
        permissions.setPublicAccess(BlobContainerPublicAccessType.OFF);
        container.uploadPermissions(permissions);
        AzureMessager.getMessager().info(String.format(UPDATE_ACCESS_LEVEL_TO_PRIVATE, DEPLOYMENT_PACKAGE_CONTAINER));
    }

    private String getBlobName(final WebAppBase deployTarget, final File zipPackage) {
        // replace '/' in resource id to '-' in case create multi-level blob
        final String fixedResourceId = StringUtils.replace(deployTarget.id(), "/", "-").replaceFirst("-", "");
        return String.format("%s-%s", fixedResourceId, zipPackage.getName());
    }
}
