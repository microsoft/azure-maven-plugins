/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.security;

import com.azure.resourcemanager.network.models.NetworkSecurityGroup.DefinitionStages.WithCreate;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@Setter
public class DraftNetworkSecurityGroup extends NetworkSecurityGroup implements AzureResourceDraft<NetworkSecurityGroup> {
    private String subscriptionId;
    private String resourceGroup;
    private String name;
    private Region region;

    public DraftNetworkSecurityGroup(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(getResourceId(subscriptionId, resourceGroup, name), null);
    }

    NetworkSecurityGroup create(final AzureNetworkSecurityGroup module) {
        this.module = module;
        WithCreate withCreate = module.getSecurityGroupManager(subscriptionId)
                .define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup);
        this.remote = withCreate.create();
        refreshStatus();
        return this;
    }

    @Override
    protected String loadStatus() {
        return Optional.ofNullable(module).map(ignore -> super.loadStatus()).orElse(IAzureBaseResource.Status.DRAFT);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.NetworkSecurityGroup loadRemote() {
        return Optional.ofNullable(module).map(ignore -> super.loadRemote()).orElse(null);
    }

    private static String getResourceId(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/virtualNetworks/%s", subscriptionId, resourceGroup, name);
    }
}
