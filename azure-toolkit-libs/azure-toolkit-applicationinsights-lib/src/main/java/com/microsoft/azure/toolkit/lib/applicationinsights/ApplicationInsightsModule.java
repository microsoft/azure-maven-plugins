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
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class ApplicationInsightsModule extends AbstractAzResourceModule<ApplicationInsight, ApplicationInsightsResourceManager, ApplicationInsightsComponent> {

    public static final String NAME = "components";

    public ApplicationInsightsModule(@Nonnull ApplicationInsightsResourceManager parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected Stream<ApplicationInsightsComponent> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ApplicationInsightsComponent loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(c -> c.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(
        name = "resource.delete_resource.resource|type",
        params = {"nameFromResourceId(resourceId)", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.deleteById(resourceId));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ApplicationInsightDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new ApplicationInsightDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected ApplicationInsightDraft newDraftForUpdate(@Nonnull ApplicationInsight applicationInsight) {
        return new ApplicationInsightDraft(applicationInsight);
    }

    @Nonnull
    @Override
    protected ApplicationInsight newResource(@Nonnull ApplicationInsightsComponent remote) {
        return new ApplicationInsight(remote, this);
    }

    @Nullable
    @Override
    public Components getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ApplicationInsightsManager::components).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Application Insights";
    }
}
