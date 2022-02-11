/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class ResourceGroupModule extends AbstractAzResourceModule<ResourceGroup, ResourceGroupManager, com.azure.resourcemanager.resources.models.ResourceGroup> {

    public static final String NAME = "resourceGroups";

    public ResourceGroupModule(@Nonnull ResourceGroupManager parent) {
        super(NAME, parent);
    }

    @AzureOperation(name = "group.create.rg", params = {"name"}, type = AzureOperation.Type.SERVICE)
    public com.microsoft.azure.toolkit.lib.common.model.ResourceGroup createResourceGroupIfNotExist(String name, Region region) {
        final com.microsoft.azure.toolkit.lib.resource.ResourceGroup group = this.getOrDraft(name, name);
        if (group instanceof ResourceGroupDraft && !group.exists()) {
            ((ResourceGroupDraft) group).setRegion(region);
            return ((ResourceGroupDraft) group).createIfNotExist().toPojo();
        } else {
            return group.toPojo();
        }
    }

    @Override
    @Nullable
    public ResourceGroups getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ResourceManager::resourceGroups).orElse(null);
    }

    public boolean exists(String resourceName) {
        return Optional.ofNullable(this.getClient()).map(c -> c.contain(resourceName)).orElse(false);
    }

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ResourceGroupDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new ResourceGroupDraft(name, resourceGroupName, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected ResourceGroupDraft newDraftForUpdate(@Nonnull ResourceGroup origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new ResourceGroupDraft(origin);
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        assert StringUtils.equalsAny(resourceGroup, resourceName, null);
        return String.format("/subscriptions/%s/resourceGroups/%s", this.getSubscriptionId(), resourceName);
    }

    @Nonnull
    protected ResourceGroup newResource(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup r) {
        return new ResourceGroup(r, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Resource group";
    }
}
