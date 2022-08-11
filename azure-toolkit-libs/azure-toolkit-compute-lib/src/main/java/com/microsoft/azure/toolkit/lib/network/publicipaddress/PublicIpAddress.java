/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.publicipaddress;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.network.NetworkServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class PublicIpAddress extends AbstractAzResource<PublicIpAddress, NetworkServiceSubscription, com.azure.resourcemanager.network.models.PublicIpAddress> {

    protected PublicIpAddress(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull PublicIpAddressModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected PublicIpAddress(@Nonnull PublicIpAddress origin) {
        super(origin);
    }

    protected PublicIpAddress(@Nonnull com.azure.resourcemanager.network.models.PublicIpAddress remote, @Nonnull PublicIpAddressModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, PublicIpAddress, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.network.models.PublicIpAddress remote) {
        return remote.innerModel().provisioningState().toString();
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    public boolean hasAssignedNetworkInterface() {
        return this.remoteOptional().map(com.azure.resourcemanager.network.models.PublicIpAddress::hasAssignedNetworkInterface).orElse(false);
    }

    @Nullable
    public String getLeafDomainLabel() {
        return remoteOptional().map(com.azure.resourcemanager.network.models.PublicIpAddress::leafDomainLabel).orElse(null);
    }
}
