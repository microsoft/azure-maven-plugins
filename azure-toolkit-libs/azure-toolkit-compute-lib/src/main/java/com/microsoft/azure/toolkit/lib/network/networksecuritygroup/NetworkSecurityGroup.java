/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.networksecuritygroup;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.network.NetworkServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class NetworkSecurityGroup extends AbstractAzResource<NetworkSecurityGroup, NetworkServiceSubscription, com.azure.resourcemanager.network.models.NetworkSecurityGroup> {

    protected NetworkSecurityGroup(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull NetworkSecurityGroupModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected NetworkSecurityGroup(@Nonnull NetworkSecurityGroup origin) {
        super(origin);
    }

    protected NetworkSecurityGroup(@Nonnull com.azure.resourcemanager.network.models.NetworkSecurityGroup remote, @Nonnull NetworkSecurityGroupModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, NetworkSecurityGroup, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.network.models.NetworkSecurityGroup remote) {
        return remote.innerModel().provisioningState().toString();
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }
}
