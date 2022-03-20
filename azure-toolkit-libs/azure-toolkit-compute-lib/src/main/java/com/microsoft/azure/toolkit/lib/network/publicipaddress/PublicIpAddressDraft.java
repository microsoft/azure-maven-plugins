/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.publicipaddress;

import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.PublicIpAddress.DefinitionStages;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class PublicIpAddressDraft extends PublicIpAddress implements AzResource.Draft<PublicIpAddress, com.azure.resourcemanager.network.models.PublicIpAddress> {
    @Nullable
    private final PublicIpAddress origin;
    @Nullable
    @Getter(AccessLevel.NONE)
    private Region region;
    @Nullable
    @Getter(AccessLevel.NONE)
    private String leafDomainLabel;

    PublicIpAddressDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull PublicIpAddressModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    PublicIpAddressDraft(@Nonnull PublicIpAddress origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.region = null;
        this.leafDomainLabel = null;
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.network.models.PublicIpAddress createResourceInAzure() {
        final String name = this.getName();
        final Region region = Objects.requireNonNull(this.getRegion(), "'region' is required to create a Public IP address");

        final NetworkManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final DefinitionStages.WithCreate create = manager.publicIpAddresses().define(name)
            .withRegion(region.getName())
            .withExistingResourceGroup(this.getResourceGroupName())
            .withLeafDomainLabel(this.getLeafDomainLabel());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Public IP address ({0})...", name));
        com.azure.resourcemanager.network.models.PublicIpAddress address = this.doModify(() -> create.create(), Status.CREATING);
        messager.success(AzureString.format("Public IP address ({0}) is successfully created", name));
        return Objects.requireNonNull(address);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.network.models.PublicIpAddress updateResourceInAzure(@Nonnull com.azure.resourcemanager.network.models.PublicIpAddress origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(this.region).orElseGet(super::getRegion);
    }

    @Nullable
    public String getLeafDomainLabel() {
        return Optional.ofNullable(this.leafDomainLabel).orElseGet(super::getLeafDomainLabel);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.region) || Objects.equals(this.region, super.getRegion()) ||
            Objects.isNull(this.leafDomainLabel) || Objects.equals(this.leafDomainLabel, super.getLeafDomainLabel());
        return !notModified;
    }

    public static String generateDefaultName() {
        return String.format("public-ip-%s", Utils.getTimestamp());
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config {
        private Region region;
        private String leafDomainLabel;
    }
}