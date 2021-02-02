/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.applicationinsights;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationType;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.resources.Provider;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import org.apache.commons.lang3.StringUtils;

import java.net.Proxy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ApplicationInsightsManager {

    private static final String MICROSOFT_INSIGHTS = "microsoft.insights";
    private static final String REGISTERED = "Registered";
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

    public ApplicationInsightsComponent createApplicationInsights(String resourceGroup, String name, String location) throws AzureExecutionException {
        registerResourceProvider();
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

    private void registerResourceProvider() throws AzureExecutionException {
        final Provider insightsProvider = azure.providers().getByName("microsoft.insights");
        if (insightsProvider != null && StringUtils.equalsIgnoreCase(insightsProvider.registrationState(), REGISTERED)) {
            return;
        }
        azure.providers().register(MICROSOFT_INSIGHTS);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future future = executorService.submit(() -> {
            try {
                Provider provider = null;
                do {
                    Thread.sleep(1000);
                    provider = azure.providers().getByName("microsoft.insights");
                } while (!StringUtils.equalsIgnoreCase(provider.registrationState(), REGISTERED));
                return provider;
            } catch (InterruptedException e) {
                // swallow interrupt exception
                return null;
            }
        });
        try {
            future.get(5, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AzureExecutionException("Failed to register provider `microsoft.insights`.");
        }
    }
}
