/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.network;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureModule;
import com.microsoft.azure.toolkit.lib.compute.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.compute.network.model.Subnet;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
public class Network extends AbstractAzureResource<com.azure.resourcemanager.network.models.Network, IAzureBaseResource> {
    protected AzureNetwork module;

    public Network(@Nonnull String id, @Nullable final AzureNetwork module) {
        super(id);
        this.module = module;
    }

    public Network(@Nonnull final com.azure.resourcemanager.network.models.Network resource, @Nonnull final AzureNetwork module) {
        super(resource);
        this.module = module;
    }

    public List<Subnet> subnets() {
        return remote.subnets().values().stream().map(Subnet::new).collect(Collectors.toList());
    }

    @Override
    public IAzureModule<? extends AbstractAzureResource<com.azure.resourcemanager.network.models.Network, IAzureBaseResource>,
            ? extends IAzureBaseResource> module() {
        return module;
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.Network loadRemote() {
        return module.getNetworkManager(getSubscriptionId()).getByResourceGroup(getResourceGroup(), getName());
    }
}
