/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.virtualnetwork;

import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.Networks;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.network.NetworkResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class NetworkModule extends AbstractAzResourceModule<Network, NetworkResourceManager, com.azure.resourcemanager.network.models.Network> {

    public static final String NAME = "virtualNetworks";

    public NetworkModule(@Nonnull NetworkResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public Networks getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(NetworkManager::networks).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected NetworkDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new NetworkDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected NetworkDraft newDraftForUpdate(@Nonnull Network origin) {
        return new NetworkDraft(origin);
    }

    @Nonnull
    protected Network newResource(@Nonnull com.azure.resourcemanager.network.models.Network r) {
        return new Network(r, this);
    }

    @Nonnull
    protected Network newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Network(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Virtual network";
    }
}
