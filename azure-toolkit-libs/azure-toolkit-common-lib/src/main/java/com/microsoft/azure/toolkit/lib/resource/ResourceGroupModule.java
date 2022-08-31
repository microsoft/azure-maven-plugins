/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceGroupModule extends AbstractAzResourceModule<ResourceGroup, ResourcesServiceSubscription, com.azure.resourcemanager.resources.models.ResourceGroup> {

    public static final String NAME = "resourceGroups";

    public ResourceGroupModule(@Nonnull ResourcesServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    public List<ResourceGroup> list() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        // FIXME: @wamgmi, improve this hotfix of #1980254
        return super.list().stream().filter(rg -> account.getSelectedSubscriptions().contains(rg.getSubscription())).collect(Collectors.toList());
    }

    @Nonnull
    @AzureOperation(name = "group.create.rg", params = {"name"}, type = AzureOperation.Type.SERVICE)
    public ResourceGroup createResourceGroupIfNotExist(@Nonnull String name, @Nonnull Region region) {
        final com.microsoft.azure.toolkit.lib.resource.ResourceGroup group = this.getOrDraft(name, name);
        if (group instanceof ResourceGroupDraft && !group.exists()) {
            ((ResourceGroupDraft) group).setRegion(region);
            return ((ResourceGroupDraft) group).createIfNotExist();
        } else {
            return group;
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

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ResourceGroupDraft newDraftForCreate(@Nonnull String name, @Nonnull String resourceGroupName) {
        return new ResourceGroupDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected ResourceGroupDraft newDraftForUpdate(@Nonnull ResourceGroup origin) {
        return new ResourceGroupDraft(origin);
    }

    @Nonnull
    protected ResourceGroup newResource(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup r) {
        return new ResourceGroup(r, this);
    }

    @Nonnull
    protected ResourceGroup newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ResourceGroup(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Resource group";
    }
}
