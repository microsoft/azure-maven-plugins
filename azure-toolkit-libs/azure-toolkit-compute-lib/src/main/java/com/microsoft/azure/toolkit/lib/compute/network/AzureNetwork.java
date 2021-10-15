/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.network;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.network.models.Networks;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.AbstractAzureResourceModule;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureNetwork extends AbstractAzureResourceModule<Network> implements AzureOperationEvent.Source<AzureNetwork> {
    public AzureNetwork() {
        super(AzureNetwork::new);
    }

    private AzureNetwork(@Nonnull final List<Subscription> subscriptions) {
        super(AzureNetwork::new, subscriptions);
    }

    @Override
    public List<Network> list(@Nonnull String subscriptionId) {
        return getNetworkManager(subscriptionId).list().stream().map(network -> new Network(network, this)).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public Network get(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        final Networks networks = getNetworkManager(subscriptionId);
        return new Network(networks.getByResourceGroup(resourceGroup, name), this);
    }

    public Network create(@Nonnull final DraftNetwork draftNetwork) {
        return draftNetwork.create(this);
    }

    Networks getNetworkManager(@Nonnull final String subscriptionId) {
        return getResourceManager(subscriptionId, ComputeManager::configure, ComputeManager.Configurable::authenticate).networkManager().networks();
    }

    @Override
    public String name() {
        return "Network";
    }
}
