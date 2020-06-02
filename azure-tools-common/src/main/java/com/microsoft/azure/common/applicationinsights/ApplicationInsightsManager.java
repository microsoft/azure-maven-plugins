/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.applicationinsights;

import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationType;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;

public class ApplicationInsightsManager {

    private Azure azure;
    private InsightsManager insightsManager;

    public ApplicationInsightsManager(AzureTokenCredentials tokenCredentials, String subscriptionId, String userAgent) {
        azure = Azure.configure()
                .withUserAgent(userAgent)
                .authenticate(tokenCredentials).withSubscription(subscriptionId);
        insightsManager = InsightsManager.configure()
                .withUserAgent(userAgent)
                .authenticate(tokenCredentials, subscriptionId);
    }

    public ApplicationInsightsComponent getApplicationInsightsInstance(String resourceGroup, String name) {
        try {
            return insightsManager.components().getByResourceGroup(resourceGroup, name);
        } catch (Exception e) {
            // SDK will throw exception when resource not found
            return null;
        }
    }

    public ApplicationInsightsComponent createApplicationInsights(String resourceGroup, String name, String location) {
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
