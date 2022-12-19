/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.resources.models.GenericResources;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class GenericResourceModule extends
    AbstractAzResourceModule<GenericResource, ResourceGroup, HasId> {

    public static final String NAME = "genericResources";

    public GenericResourceModule(@Nonnull ResourceGroup parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    public GenericResources getClient() {
        return Optional.ofNullable(this.parent.getParent().getRemote()).map(ResourceManager::genericResources).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<HasId> loadResourcesFromAzure() {
        final GenericResources resources = Objects.requireNonNull(this.getClient());
        return resources.listByResourceGroup(this.parent.getName()).stream()
            .filter(r -> Objects.isNull(ResourceId.fromString(r.id()).parent())).map(r -> r); // only keep top resources.
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceId, @Nullable String resourceGroup) {
        return resourceId;
    }

    @Nonnull
    protected GenericResource newResource(@Nonnull HasId r) {
        return new GenericResource(r, this);
    }

    @Nonnull
    protected GenericResource newResource(@Nonnull String resourceId, @Nullable String resourceGroupName) {
        return new GenericResource(resourceId, this);
    }

    @Nonnull
    public GenericResource newResource(@Nonnull AbstractAzResource<?, ?, ?> concrete) {
        return new GenericResource(concrete, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Generic resource";
    }
}
