/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.publicipaddress;

import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.PublicIpAddresses;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.network.NetworkServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class PublicIpAddressModule extends AbstractAzResourceModule<PublicIpAddress, NetworkServiceSubscription, com.azure.resourcemanager.network.models.PublicIpAddress> {

    public static final String NAME = "publicIPAddresses";

    public PublicIpAddressModule(@Nonnull NetworkServiceSubscription parent) {
        super(NAME, parent);
    }

    @Override
    public PublicIpAddresses getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(NetworkManager::publicIpAddresses).orElse(null);
    }

    @Nonnull
    @Override
    protected PublicIpAddressDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new PublicIpAddressDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected PublicIpAddressDraft newDraftForUpdate(@Nonnull PublicIpAddress origin) {
        return new PublicIpAddressDraft(origin);
    }

    @Nonnull
    protected PublicIpAddress newResource(@Nonnull com.azure.resourcemanager.network.models.PublicIpAddress r) {
        return new PublicIpAddress(r, this);
    }

    @Nonnull
    protected PublicIpAddress newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new PublicIpAddress(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Public IP address";
    }
}
