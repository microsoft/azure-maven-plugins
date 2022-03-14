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
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;


public class ContainerRegistryDraft extends ContainerRegistry implements AzResource.Draft<ContainerRegistry, Registry> {

    private static final String REGION_AND_SKU_IS_REQUIRED = "Region and sku is required to create registry in Azure";

    @Getter
    @Setter
    private Region region;

    @Getter
    @Setter
    private Sku sku;

    @Getter
    @Setter
    private Boolean isAdminUserEnabled;

    @Getter
    @Setter
    private Boolean isPublicAccessEnabled;

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
        this.isAdminUserEnabled = null;
        this.isPublicAccessEnabled = null;
    }

    @Override
    public Registry createResourceInAzure() {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        if (ObjectUtils.allNull(region, sku)) {
            throw new AzureToolkitRuntimeException(REGION_AND_SKU_IS_REQUIRED);
        }
        final Registries registries = Objects.requireNonNull(this.getParent().getAzureContainerRegistryModule().getClient());
        final Registry.DefinitionStages.WithSku withSku = registries.define(this.getName())
                .withRegion(this.getRegion().getName())
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
        if (isAdminUserEnabled) {
            withCreate.withRegistryNameAsAdminUser();
        }
        if (!isPublicAccessEnabled) {
            withCreate.disablePublicNetworkAccess();
        }
        AzureMessager.getMessager().info(AzureString.format("Start creating Container Registry ({0})", getName()));
        final Registry registry = withCreate.create();
        AzureMessager.getMessager().success(AzureString.format("Container Registry ({0}) is successfully created", getName()));
        return registry;
    }

    @Override
    public Registry updateResourceInAzure(@Nonnull Registry origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
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
        if (isAdminUserEnabled != null) {
            if (isAdminUserEnabled == Boolean.TRUE) {
                update.withRegistryNameAsAdminUser();
            } else {
                update.withoutRegistryNameAsAdminUser();
            }
        }
        if (isPublicAccessEnabled != null) {
            if (isPublicAccessEnabled == Boolean.TRUE) {
                update.enablePublicNetworkAccess();
            } else {
                update.disablePublicNetworkAccess();
            }
        }
        return update.apply();
    }

    @Override
    public boolean isModified() {
        if (origin == null) {
            return ObjectUtils.anyNotNull(this.region, this.sku, this.isAdminUserEnabled, this.isPublicAccessEnabled);
        }
        return (this.region != null && !Objects.equals(this.region, this.origin.getRegion())) ||
                (this.sku != null && !Objects.equals(this.sku, this.origin.getSku())) ||
                (this.isPublicAccessEnabled != null && !Objects.equals(this.isPublicAccessEnabled, this.origin.isPublicAccessEnabled())) ||
                (this.isAdminUserEnabled != null && !Objects.equals(this.isAdminUserEnabled, this.origin.isAdminUserEnabled()));
    }

    @Nullable
    @Override
    public ContainerRegistry getOrigin() {
        return this.origin;
    }
}
