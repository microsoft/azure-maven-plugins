/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network;

import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.network.networksecuritygroup.NetworkSecurityGroupModule;
import com.microsoft.azure.toolkit.lib.network.publicipaddress.PublicIpAddressModule;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.NetworkModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public class NetworkServiceSubscription extends AbstractAzServiceSubscription<NetworkServiceSubscription, NetworkManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final NetworkSecurityGroupModule networkSecurityGroupModule;
    @Nonnull
    private final PublicIpAddressModule publicIpAddressModule;
    @Nonnull
    private final NetworkModule networkModule;

    NetworkServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureNetwork service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.networkSecurityGroupModule = new NetworkSecurityGroupModule(this);
        this.publicIpAddressModule = new PublicIpAddressModule(this);
        this.networkModule = new NetworkModule(this);
    }

    NetworkServiceSubscription(@Nonnull NetworkManager remote, @Nonnull AzureNetwork service) {
        this(remote.subscriptionId(), service);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, NetworkServiceSubscription, ?>> getSubModules() {
        return Arrays.asList(networkSecurityGroupModule, publicIpAddressModule, networkModule);
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }
}

