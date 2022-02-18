/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.azure.resourcemanager.applicationinsights.models.Components;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class ApplicationInsightsModule extends AbstractAzResourceModule<ApplicationInsight, ApplicationInsightsResourceManager, ApplicationInsightsComponent> {

    public static final String NAME = "applicationInsights";

    public ApplicationInsightsModule(@Nonnull ApplicationInsightsResourceManager parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Stream<ApplicationInsightsComponent> loadResourcesFromAzure() {
        return this.getClient().list().stream();
    }

    @Nullable
    @Override
    protected ApplicationInsightsComponent loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        return this.getClient().getByResourceGroup(resourceGroup, name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        this.getClient().deleteById(resourceId);
    }

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ApplicationInsightDraft newDraftForCreate(@Nonnull String name, String resourceGroup) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new ApplicationInsightDraft(name, resourceGroup, this);
    }

    @Override
    @AzureOperation(
            name = "resource.draft_for_update.resource|type",
            params = {"origin.getName()", "this.getResourceTypeName()"},
            type = AzureOperation.Type.SERVICE
    )
    protected ApplicationInsightDraft newDraftForUpdate(@Nonnull ApplicationInsight applicationInsight) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new ApplicationInsightDraft(applicationInsight);
    }

    @Override
    protected ApplicationInsight newResource(@Nonnull ApplicationInsightsComponent remote) {
        return new ApplicationInsight(remote, this);
    }

    @Override
    public Components getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ApplicationInsightsManager::components).orElse(null);
    }


    @Override
    public String getResourceTypeName() {
        return "Azure Application Insights";
    }
}
