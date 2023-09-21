/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.deploy;

import com.azure.core.http.policy.HttpLogOptions;
import com.azure.resourcemanager.appservice.models.AppSetting;
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

class DeployUtils {
    private static final String INTERNAL_STORAGE_NOT_FOUND = "Application setting 'AzureWebJobsStorage' is not found, " +
        "please check the application setting and try again later.";
    public static final String INVALID_STORAGE_CONNECTION_STRING = "Storage connection string in 'AzureWebJobsStorage' is invalid, " +
        "please check the application setting and try again later.";
    private static final String INTERNAL_STORAGE_KEY = "AzureWebJobsStorage";
    private static final String UNSUPPORTED_DEPLOYMENT_TARGET = "Unsupported deployment target, only function is supported";

    /**
     * Get storage account specified within AzureWebJobsStorage for function app/slot
     *
     * @param functionApp target function/slot, using WebAppBase here which is the base class for function app/slot in sdk
     * @return StorageAccount specified in AzureWebJobsStorage
     */
    static BlobServiceClient getBlobServiceClient(final WebAppBase functionApp) {
        // Call functionApp.getSiteAppSettings() to get the app settings with key vault reference
        final String connectionString = Optional.ofNullable(functionApp.getSiteAppSettings())
            .map(map -> map.get(INTERNAL_STORAGE_KEY))
            .filter(StringUtils::isNotEmpty)
            .orElseGet(() -> Optional.ofNullable(functionApp.getAppSettings())
                .map(map -> map.get(INTERNAL_STORAGE_KEY)).map(AppSetting::value).orElse(null));
        if (StringUtils.isEmpty(connectionString)) {
            throw new AzureToolkitRuntimeException(INTERNAL_STORAGE_NOT_FOUND);
        }
        return new BlobServiceClientBuilder()
            .httpLogOptions(new HttpLogOptions().setLogLevel(Azure.az().config().getLogLevel()))
            .addPolicy(Azure.az().config().getUserAgentPolicy())
            .connectionString(connectionString)
            .buildClient();
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
