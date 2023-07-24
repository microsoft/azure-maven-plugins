/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerAccessPolicies;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.function.AzureStorageHelper;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.Period;

import static com.microsoft.azure.toolkit.lib.legacy.function.Constants.APP_SETTING_WEBSITE_RUN_FROM_PACKAGE;

public class RunFromBlobFunctionDeployHandler implements IFunctionDeployHandler {
    private static final int SAS_EXPIRE_DATE_BY_YEAR = 10;
    private static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-run-from-packages";
    private static final String UPDATE_ACCESS_LEVEL_TO_PRIVATE = "The blob container '%s' access level was updated to be private";

    @Override
    public void deploy(@Nonnull File file, @Nonnull WebAppBase target) {
        final BlobServiceClient storageAccount = DeployUtils.getBlobServiceClient(target);
        final BlobClient blob = deployArtifactToAzureStorage(target, file, storageAccount);
        final String sasToken = AzureStorageHelper.getSASToken(blob, Period.ofYears(SAS_EXPIRE_DATE_BY_YEAR));
        DeployUtils.updateFunctionAppSetting(target, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, sasToken);
    }

    private BlobClient deployArtifactToAzureStorage(WebAppBase deployTarget, File zipPackage, BlobServiceClient storageAccount) {
        AzureMessager.getMessager().info(String.format(DEPLOY_START, deployTarget.name()));
        final BlobContainerClient container = getOrCreateArtifactContainer(storageAccount);
        final String blobName = getBlobName(deployTarget, zipPackage);
        final BlobClient blob = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                container.getBlobContainerName(), blobName);
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, deployTarget.defaultHostname()));
        return blob;
    }

    private BlobContainerClient getOrCreateArtifactContainer(final BlobServiceClient storageAccount) {
        final BlobContainerClient container = storageAccount.getBlobContainerClient(DEPLOYMENT_PACKAGE_CONTAINER);
        if (!container.exists()) {
            container.createIfNotExists();
        } else {
            updateContainerPublicAccessLevel(container);
        }
        return container;
    }

    private void updateContainerPublicAccessLevel(final BlobContainerClient container)  {
        final BlobContainerAccessPolicies permissions = container.getAccessPolicy();
        if (permissions.getBlobAccessType() == null) {
            return;
        }
        container.setAccessPolicy(null, permissions.getIdentifiers());
        AzureMessager.getMessager().info(String.format(UPDATE_ACCESS_LEVEL_TO_PRIVATE, DEPLOYMENT_PACKAGE_CONTAINER));
    }

    private String getBlobName(final WebAppBase deployTarget, final File zipPackage) {
        // replace '/' in resource id to '-' in case create multi-level blob
        final String fixedResourceId = StringUtils.replace(deployTarget.id(), "/", "-").replaceFirst("-", "");
        return String.format("%s-%s", fixedResourceId, zipPackage.getName());
    }
}
