/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.security;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.network.models.NetworkSecurityGroups;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AbstractAzureResourceModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class AzureNetworkSecurityGroup extends AbstractAzureResourceModule<NetworkSecurityGroup> {
    public AzureNetworkSecurityGroup() {
        super(AzureNetworkSecurityGroup::new);
    }

    private AzureNetworkSecurityGroup(@Nullable List<Subscription> subscriptions) {
        super(AzureNetworkSecurityGroup::new, subscriptions);
    }

    @Override
    public List<NetworkSecurityGroup> list(@Nonnull String subscriptionId) {
        return getSecurityGroupManager(subscriptionId).list().stream().map(group -> new NetworkSecurityGroup(group, this)).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public NetworkSecurityGroup get(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        final NetworkSecurityGroups securityGroupManager = getSecurityGroupManager(subscriptionId);
        return new NetworkSecurityGroup(securityGroupManager.getByResourceGroup(resourceGroup, name), this);
    }

    public NetworkSecurityGroup create(@Nonnull final DraftNetworkSecurityGroup draftNetworkSecurityGroup) {
        return draftNetworkSecurityGroup.create(this);
    }

    public NetworkSecurityGroups getSecurityGroupManager(final String subscriptionId) {
        return getResourceManager(subscriptionId, ComputeManager::configure, ComputeManager.Configurable::authenticate).networkManager().networkSecurityGroups();
    }

    @Override
    public String name() {
        return "NetworkSecurityGroup";
    }
}
