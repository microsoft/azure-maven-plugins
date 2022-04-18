/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.networksecuritygroup;

import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.NetworkSecurityGroups;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.network.NetworkServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class NetworkSecurityGroupModule extends AbstractAzResourceModule<NetworkSecurityGroup, NetworkServiceSubscription, com.azure.resourcemanager.network.models.NetworkSecurityGroup> {

    public static final String NAME = "networkSecurityGroups";

    public NetworkSecurityGroupModule(@Nonnull NetworkServiceSubscription parent) {
        super(NAME, parent);
    }

    @Override
    public NetworkSecurityGroups getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(NetworkManager::networkSecurityGroups).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected NetworkSecurityGroupDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new NetworkSecurityGroupDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected NetworkSecurityGroupDraft newDraftForUpdate(@Nonnull NetworkSecurityGroup origin) {
        return new NetworkSecurityGroupDraft(origin);
    }

    @Nonnull
    protected NetworkSecurityGroup newResource(@Nonnull com.azure.resourcemanager.network.models.NetworkSecurityGroup r) {
        return new NetworkSecurityGroup(r, this);
    }

    @Nonnull
    protected NetworkSecurityGroup newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new NetworkSecurityGroup(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Network security group";
    }
}
