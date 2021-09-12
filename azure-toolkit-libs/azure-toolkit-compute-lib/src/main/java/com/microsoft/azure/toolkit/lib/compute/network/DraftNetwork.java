/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.network;

import com.azure.resourcemanager.network.models.Network.DefinitionStages.WithCreateAndSubnet;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.network.model.Subnet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DraftNetwork extends Network implements AzureResourceDraft<Network> {

    private String subscriptionId;
    private String resourceGroup;
    private String name;
    private Region region;
    private String addressSpace;

    private String subnet;
    private String subnetAddressSpace;

    public DraftNetwork(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(getResourceId(subscriptionId, resourceGroup, name), null);
    }

    Network create(final AzureNetwork module) {
        this.module = module;
        WithCreateAndSubnet withCreateAndSubnet = module.getNetworkManager(subscriptionId).define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup)
                .withAddressSpace(addressSpace);
        if (StringUtils.isNotEmpty(subnet)) {
            withCreateAndSubnet = withCreateAndSubnet.withSubnet(subnet, subnetAddressSpace);
        }
        this.remote = withCreateAndSubnet.create();
        refreshStatus();
        return this;
    }

    @Override
    public List<Subnet> subnets() {
        return Optional.ofNullable(module).map(ignore -> super.subnets()).orElseGet(() -> Collections.singletonList(new Subnet(subnet, subnetAddressSpace)));
    }

    @Override
    protected String loadStatus() {
        return Optional.ofNullable(module).map(ignore -> super.loadStatus()).orElse(Status.DRAFT);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.Network loadRemote() {
        return Optional.ofNullable(module).map(ignore -> super.loadRemote()).orElse(null);
    }

    private static String getResourceId(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/virtualNetworks/%s", subscriptionId, resourceGroup, name);
    }
}
