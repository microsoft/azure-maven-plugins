/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.security;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureModule;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.compute.AbstractAzureResource;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NetworkSecurityGroup extends AbstractAzureResource<com.azure.resourcemanager.network.models.NetworkSecurityGroup, IAzureBaseResource>
        implements AzureOperationEvent.Source<NetworkSecurityGroup> {

    protected AzureNetworkSecurityGroup module;

    public NetworkSecurityGroup(@Nonnull final String id, @Nullable final AzureNetworkSecurityGroup module) {
        super(id);
        this.module = module;
    }

    public NetworkSecurityGroup(@Nonnull final com.azure.resourcemanager.network.models.NetworkSecurityGroup resource,
                                @Nonnull final AzureNetworkSecurityGroup module) {
        super(resource);
        this.module = module;
    }

    @Override
    public IAzureModule<? extends AbstractAzureResource<com.azure.resourcemanager.network.models.NetworkSecurityGroup, IAzureBaseResource>,
            ? extends IAzureBaseResource> module() {
        return module;
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.NetworkSecurityGroup loadRemote() {
        return module.getSecurityGroupManager(getSubscriptionId()).getByResourceGroup(getResourceGroup(), getName());
    }
}
