/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.applicationinsights;

import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationType;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;

import java.io.IOException;

public class ApplicationInsightsManager {

    private Azure azure;
    private InsightsManager insightsManager;
    private ApplicationInsightsManagementClient aiClient;

    public ApplicationInsightsManager(AzureTokenCredentials tokenCredentials, String userAgent) throws AzureExecutionException {
        try {
            insightsManager = InsightsManager.authenticate(tokenCredentials, tokenCredentials.defaultSubscriptionId());
            azure = Azure.configure().authenticate(tokenCredentials).withDefaultSubscription();
        } catch (IOException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }

    public ApplicationInsightsComponent getApplicationInsightInstance(String resourceGroup, String name) {
        return insightsManager.components().getByResourceGroup(resourceGroup, name);
    }

    public ApplicationInsightsComponent createApplicationInsight(String resourceGroup, String name, String location) {
        if (!azure.resourceGroups().contain(resourceGroup)) {
            azure.resourceGroups().define(resourceGroup).withRegion(location).create();
        }
        return insightsManager
                .components()
                .define(name)
                .withRegion(location)
                .withExistingResourceGroup(resourceGroup)
                .withApplicationType(ApplicationType.WEB)
                .withKind("web")
                .create();
    }
}
