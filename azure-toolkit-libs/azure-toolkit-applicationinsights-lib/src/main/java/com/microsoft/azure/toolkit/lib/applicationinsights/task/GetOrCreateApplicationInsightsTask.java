/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightDraft;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightsModule;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;

import javax.annotation.Nonnull;
import java.util.Optional;

public class GetOrCreateApplicationInsightsTask extends AzureTask<ApplicationInsight> {
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
    public ApplicationInsight execute() {
        Azure.az(AzureResources.class).groups(subscriptionId).createResourceGroupIfNotExist(this.resourceGroup, this.region);
        final ApplicationInsightsModule insightsModule = Azure.az(AzureApplicationInsights.class).applicationInsights(subscriptionId);
        return Optional.ofNullable(insightsModule.get(name, this.resourceGroup)).orElseGet(() -> {
            final ApplicationInsightDraft draft = insightsModule.create(name, this.resourceGroup);
            draft.setRegion(region);
            return draft.commit();
        });
    }
}
