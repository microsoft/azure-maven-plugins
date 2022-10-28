/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.resourcemanager.containerregistry.models.Registries;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;


public class ContainerRegistryDraft extends ContainerRegistry implements AzResource.Draft<ContainerRegistry, Registry> {
    private static final String REGION_AND_SKU_IS_REQUIRED = "`region` and `sku` is required to create a container registry in Azure";

    @Setter
    @Nullable
    private Region region;

    @Setter
    @Nullable
    private Sku sku;

    @Setter
    @Nullable
    private Boolean adminUserEnabled;

    @Setter
    @Nullable
    private Boolean publicAccessEnabled;

    @Nullable
    private final ContainerRegistry origin;

    protected ContainerRegistryDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AzureContainerRegistryModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    public ContainerRegistryDraft(@Nonnull ContainerRegistry origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.region = null;
        this.sku = null;
        this.adminUserEnabled = null;
        this.publicAccessEnabled = null;
    }

    @Override
    @Nonnull
    @AzureOperation(name = "container.create_registry.registry", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public Registry createResourceInAzure() {
        if (ObjectUtils.anyNull(region, sku)) {
            throw new AzureToolkitRuntimeException(REGION_AND_SKU_IS_REQUIRED);
        }
        final Registries registries = Objects.requireNonNull(this.getParent().getAzureContainerRegistryModule().getClient());
        final Registry.DefinitionStages.WithSku withSku = registries.define(this.getName())
                .withRegion(region.getName())
                .withExistingResourceGroup(this.getResourceGroupName());
        final Registry.DefinitionStages.WithCreate withCreate;
        if (sku == Sku.Basic) {
            withCreate = withSku.withBasicSku();
        } else if (sku == Sku.Standard) {
            withCreate = withSku.withStandardSku();
        } else if (sku == Sku.Premium) {
            withCreate = withSku.withPremiumSku();
        } else {
            throw new AzureToolkitRuntimeException(String.format("Invalid sku, valid values are %s", StringUtils.join(Sku.values(), ",")));
        }
        if (isAdminUserEnabled()) {
            withCreate.withRegistryNameAsAdminUser();
        }
        if (!isPublicAccessEnabled()) {
            withCreate.disablePublicNetworkAccess();
        }
        AzureMessager.getMessager().info(AzureString.format("Start creating Container Registry ({0})", getName()));
        final Registry registry = withCreate.create();
        AzureMessager.getMessager().success(AzureString.format("Container Registry ({0}) is successfully created", getName()));
        return registry;
    }

    @Override
    @Nonnull
    @AzureOperation(name = "container.update_registry.registry", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public Registry updateResourceInAzure(@Nonnull Registry origin) {
        if (!isModified()) {
            return origin;
        }
        final Registry.Update update = origin.update();
        if (this.sku != null) {
            if (sku == Sku.Basic) {
                update.withBasicSku();
            } else if (sku == Sku.Standard) {
                update.withStandardSku();
            } else if (sku == Sku.Premium) {
                update.withPremiumSku();
            } else {
                throw new AzureToolkitRuntimeException(String.format("Invalid sku, valid values are %s", StringUtils.join(Sku.values(), ",")));
            }
        }
        if (isAdminUserEnabled()) {
            update.withRegistryNameAsAdminUser();
        } else {
            update.withoutRegistryNameAsAdminUser();
        }
        if (isPublicAccessEnabled()) {
            update.enablePublicNetworkAccess();
        } else {
            update.disablePublicNetworkAccess();
        }
        return update.apply();
    }

    @Override
    public boolean isModified() {
        if (origin == null) {
            return ObjectUtils.anyNotNull(this.region, this.sku, this.publicAccessEnabled, this.adminUserEnabled);
        }
        return !(Objects.equals(this.getRegion(), this.origin.getRegion()) &&
                Objects.equals(this.getSku(), this.origin.getSku()) &&
                Objects.equals(this.isPublicAccessEnabled(), this.origin.isPublicAccessEnabled()) &&
                Objects.equals(this.isAdminUserEnabled(), this.origin.isAdminUserEnabled()));
    }

    @Nullable
    @Override
    public ContainerRegistry getOrigin() {
        return this.origin;
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(region).orElseGet(() -> Optional.ofNullable(origin).map(ContainerRegistry::getRegion).orElse(null));
    }

    @Nullable
    public Sku getSku() {
        return Optional.ofNullable(sku).orElseGet(() -> Optional.ofNullable(origin).map(ContainerRegistry::getSku).orElse(null));
    }

    @Override
    public boolean isAdminUserEnabled() {
        return Optional.ofNullable(adminUserEnabled).orElseGet(super::isAdminUserEnabled);
    }

    @Override
    public boolean isPublicAccessEnabled() {
        return Optional.ofNullable(publicAccessEnabled).orElseGet(super::isPublicAccessEnabled);
    }
}
