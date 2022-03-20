/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.virtualnetwork;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.network.NetworkResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Network extends AbstractAzResource<Network, NetworkResourceManager, com.azure.resourcemanager.network.models.Network> {

    protected Network(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull NetworkModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected Network(@Nonnull Network origin) {
        super(origin);
    }

    protected Network(@Nonnull com.azure.resourcemanager.network.models.Network remote, @Nonnull NetworkModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, Network, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.network.models.Network remote) {
        return remote.innerModel().provisioningState().toString();
    }

    @Nonnull
    @Override
    public String status() {
        return this.getStatus();
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nonnull
    public List<Subnet> getSubnets() {
        return this.remoteOptional().map(Stream::of).orElseGet(Stream::empty)
            .flatMap(n -> n.subnets().values().stream())
            .map(Subnet::new).collect(Collectors.toList());
    }

    @Nullable
    public Subnet getSubnet() {
        return this.getSubnets().stream().findFirst().orElse(null);
    }

    @Nonnull
    public List<String> getAddressSpaces() {
        return this.remoteOptional().map(Stream::of).orElseGet(Stream::empty)
            .flatMap(n -> n.addressSpaces().stream())
            .collect(Collectors.toList());
    }

    @Nullable
    public String getAddressSpace() {
        return this.getAddressSpaces().stream().findFirst().orElse(null);
    }
}
