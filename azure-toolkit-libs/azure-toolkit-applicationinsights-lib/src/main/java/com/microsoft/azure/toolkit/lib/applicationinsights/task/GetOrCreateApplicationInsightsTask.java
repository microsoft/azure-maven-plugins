/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights.task;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightsEntity;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils.getPortalUrl;

public class GetOrCreateApplicationInsightsTask extends AzureTask<ApplicationInsightsEntity> {
    private static final String APPLICATION_INSIGHTS_CREATE_START = "Creating application insights...";
    private static final String APPLICATION_INSIGHTS_CREATED = "Successfully created the application insights %s " +
            "for this Function App. You can visit %s/#@/resource%s/overview to view your " +
            "Application Insights component.";

    private final String subscriptionId;
    private final String resourceGroup;
    private final String name;
    private final Region region;

    public GetOrCreateApplicationInsightsTask(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull Region region, @Nonnull String name) {
        this.subscriptionId = subscriptionId;
        this.resourceGroup = resourceGroup;
        this.name = name;
        this.region = region;
    }

    @Override
    public ApplicationInsightsEntity execute() {
        final ApplicationInsights az = Azure.az(ApplicationInsights.class).subscription(subscriptionId);
        try {
            return az.get(resourceGroup, name);
        } catch (ManagementException e) {
            if (e.getResponse().getStatusCode() != 404) {
                throw e;
            }
        }
        AzureMessager.getMessager().info(APPLICATION_INSIGHTS_CREATE_START);
        final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
        final ApplicationInsightsEntity resource = Azure.az(ApplicationInsights.class).create(resourceGroup, region, name);
        AzureMessager.getMessager().info(String.format(APPLICATION_INSIGHTS_CREATED, resource.getName(), getPortalUrl(environment), resource.getId()));
        return resource;
    }
}
