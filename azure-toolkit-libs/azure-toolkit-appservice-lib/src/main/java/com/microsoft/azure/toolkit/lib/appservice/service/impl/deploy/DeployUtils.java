/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.AppSetting;
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Optional;

class DeployUtils {
    private static final String INTERNAL_STORAGE_NOT_FOUND = "Application setting 'AzureWebJobsStorage' not found or value is invalid, " +
            "please check the app setting and try deploy later.";
    private static final String INTERNAL_STORAGE_KEY = "AzureWebJobsStorage";
    private static final String UNSUPPORTED_DEPLOYMENT_TARGET = "Unsupported deployment target, only function is supported";

    /**
     * Get storage account specified within AzureWebJobsStorage for function app/slot
     * @param functionApp target function/slot, using WebAppBase here which is the base class for function app/slot in sdk
     * @return StorageAccount specified in AzureWebJobsStorage
     */
    static CloudStorageAccount getCloudStorageAccount(final WebAppBase functionApp) {
        // Call functionApp.getSiteAppSettings() to get the app settings with key vault reference
        final String kuduStorageSetting = Optional.ofNullable(functionApp.getSiteAppSettings())
                .map(map -> map.get(INTERNAL_STORAGE_KEY)).orElse(null);
        final String storageSetting = Optional.ofNullable(functionApp.getAppSettings())
                .map(map -> map.get(INTERNAL_STORAGE_KEY)).map(AppSetting::value).orElse(null);
        for (final String connectionString : Arrays.asList(storageSetting, kuduStorageSetting)) {
            try {
                return CloudStorageAccount.parse(connectionString);
            } catch (InvalidKeyException | URISyntaxException | RuntimeException e) {
                // swallow exception here
                continue;
            }
        }
        throw new AzureToolkitRuntimeException(INTERNAL_STORAGE_NOT_FOUND);
    }

    static void updateFunctionAppSetting(final WebAppBase deployTarget, final String key, final String value) {
        if (deployTarget instanceof FunctionApp) {
            ((FunctionApp) deployTarget).update().withAppSetting(key, value).apply();
        } else if (deployTarget instanceof FunctionDeploymentSlot) {
            ((FunctionDeploymentSlot) deployTarget).update().withAppSetting(key, value).apply();
        } else {
            throw new AzureToolkitRuntimeException(UNSUPPORTED_DEPLOYMENT_TARGET);
        }
    }
}
