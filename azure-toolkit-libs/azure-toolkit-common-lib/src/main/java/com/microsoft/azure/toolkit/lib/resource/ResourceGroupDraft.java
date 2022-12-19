/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.Data;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class ResourceGroupDraft extends ResourceGroup implements AzResource.Draft<ResourceGroup, com.azure.resourcemanager.resources.models.ResourceGroup> {
    private static final String CREATE_NEW_RESOURCE_GROUP_KEY = "createNewResourceGroup";

    @Getter
    @Nullable
    private final ResourceGroup origin;
    @Nullable
    private Config config;

    ResourceGroupDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ResourceGroupModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    ResourceGroupDraft(@Nonnull ResourceGroup origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/arm.create_group.rg", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public com.azure.resourcemanager.resources.models.ResourceGroup createResourceInAzure() {
        OperationContext.action().setTelemetryProperty(CREATE_NEW_RESOURCE_GROUP_KEY, String.valueOf(true));
        final String name = this.getName();
        final Region region = this.getRegion();
        if (Objects.isNull(region)) {
            throw new AzureToolkitRuntimeException("'region' is required to create resource group.");
        }
        final ResourceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Resource Group({0}) in region ({1})...", name, region.getLabel()));
        final com.azure.resourcemanager.resources.models.ResourceGroup group =
            manager.resourceGroups().define(name).withRegion(region.getName()).create();
        messager.success(AzureString.format("Resource Group({0}) is successfully created.", name));
        return group;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/arm.update_group.rg", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public com.azure.resourcemanager.resources.models.ResourceGroup updateResourceInAzure(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    public void setRegion(@Nonnull Region region) {
        this.ensureConfig().setRegion(region);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getRegion()) || Objects.equals(this.config.getRegion(), super.getRegion());
        return !notModified;
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config {
        private Region region;
    }
}