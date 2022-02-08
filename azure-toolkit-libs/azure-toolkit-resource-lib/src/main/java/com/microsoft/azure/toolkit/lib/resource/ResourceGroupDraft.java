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
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
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

    ResourceGroupDraft(@Nonnull String name, String resourceGroupName, @Nonnull ResourceGroupModule module) {
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

    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.resources.models.ResourceGroup createResourceInAzure() {
        AzureTelemetry.getActionContext().setProperty(CREATE_NEW_RESOURCE_GROUP_KEY, String.valueOf(true));
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        final String name = this.getName();
        final Region region = this.getRegion();
        if (Objects.isNull(region)) {
            throw new AzureToolkitRuntimeException("'region' is required to create resource group.");
        }
        final ResourceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Resource Group({0})...", name));
        final com.azure.resourcemanager.resources.models.ResourceGroup group =
            manager.resourceGroups().define(name).withRegion(region.getName()).create();
        messager.success(AzureString.format("Resource Group({0}) is successfully created.", name));
        return group;
    }

    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.resources.models.ResourceGroup updateResourceInAzure(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    public void setRegion(@Nonnull Region region) {
        this.ensureConfig().setRegion(region);
    }

    @Nullable
    public Region getRegion() {
        return Objects.requireNonNull(Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion));
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