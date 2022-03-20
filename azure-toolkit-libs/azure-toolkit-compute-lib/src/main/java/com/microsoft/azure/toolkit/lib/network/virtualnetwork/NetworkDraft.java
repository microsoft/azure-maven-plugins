/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.virtualnetwork;

import com.azure.resourcemanager.network.NetworkManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class NetworkDraft extends Network implements AzResource.Draft<Network, com.azure.resourcemanager.network.models.Network> {
    @Getter
    @Nullable
    private final Network origin;
    @Nullable
    @Getter(AccessLevel.NONE)
    private Region region;
    @Nullable
    private String addressSpace;
    @Nullable
    private Subnet subnet;

    NetworkDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull NetworkModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    NetworkDraft(@Nonnull Network origin) {
        super(origin);
        this.origin = origin;
    }

    public NetworkDraft withDefaultConfig() {
        this.setAddressSpace("10.0.2.0/24");
        this.setSubnet("default", "10.0.2.0/24");
        return this;
    }

    @Override
    public void reset() {
        this.region = null;
        this.addressSpace = null;
        this.subnet = null;
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.network.models.Network createResourceInAzure() {
        final String name = this.getName();
        final Region region = Objects.requireNonNull(this.getRegion(), "'region' is required to create a Virtual network");
        final String addressSpace = this.getAddressSpaces().stream().findFirst().orElse(null);
        final Subnet subnet = this.getSubnets().stream().findFirst().orElse(null);

        final NetworkManager manager = Objects.requireNonNull(this.getParent().getRemote());
        com.azure.resourcemanager.network.models.Network.DefinitionStages.WithCreateAndSubnet create = manager.networks().define(name)
            .withRegion(region.getName())
            .withExistingResourceGroup(this.getResourceGroupName()).withAddressSpace(addressSpace);
        if (Objects.nonNull(subnet)) {
            create = create.withSubnet(subnet.getName(), subnet.getAddressSpace());
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Virtual network ({0})...", name));
        com.azure.resourcemanager.network.models.Network.DefinitionStages.WithCreateAndSubnet finalCreate = create;
        final com.azure.resourcemanager.network.models.Network network = this.doModify(() -> finalCreate.create(), Status.CREATING);
        messager.success(AzureString.format("Virtual network ({0}) is successfully created", name));
        return Objects.requireNonNull(network);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.network.models.Network updateResourceInAzure(@Nonnull com.azure.resourcemanager.network.models.Network origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(this.region).orElseGet(super::getRegion);
    }

    public void setSubnet(@Nonnull String subnetName, @Nonnull String addressSpace) {
        this.setSubnet(new Subnet(subnetName, addressSpace));
    }

    @Nullable
    public Subnet getSubnet() {
        return Optional.ofNullable(this.subnet).orElseGet(super::getSubnet);
    }

    @Nullable
    public String getAddressSpace() {
        return Optional.ofNullable(this.addressSpace).orElseGet(super::getAddressSpace);
    }

    @Nonnull
    @Override
    public List<Subnet> getSubnets() {
        return Optional.ofNullable(this.subnet).map(Collections::singletonList).orElseGet(super::getSubnets);
    }

    @Nonnull
    @Override
    public List<String> getAddressSpaces() {
        return Optional.ofNullable(this.addressSpace).map(Collections::singletonList).orElseGet(super::getAddressSpaces);
    }

    @Override
    public boolean isModified() {
        final boolean notModified =
            Objects.isNull(this.region) || Objects.equals(this.region, super.getRegion()) ||
                Objects.isNull(this.addressSpace) ||
                Objects.isNull(this.subnet);
        return !notModified;
    }
}