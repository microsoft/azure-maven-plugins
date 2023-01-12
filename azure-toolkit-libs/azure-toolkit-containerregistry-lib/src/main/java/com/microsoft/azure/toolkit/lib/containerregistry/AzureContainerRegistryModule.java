/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.containerregistry.ContainerRegistryManager;
import com.azure.resourcemanager.containerregistry.models.Registries;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AzureContainerRegistryModule extends AbstractAzResourceModule<ContainerRegistry, AzureContainerRegistryServiceSubscription, Registry> {

    public static final String NAME = "registries";

    public AzureContainerRegistryModule(@Nonnull AzureContainerRegistryServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Registry>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    protected Stream<Registry> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(Registries::list).map(PagedIterable::stream).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected Registry loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        return Optional.ofNullable(this.getClient()).map(client -> client.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/container.delete_registry.registry", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(client -> client.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected ContainerRegistry newResource(@Nonnull Registry registry) {
        return new ContainerRegistry(registry, this);
    }

    @Nonnull
    @Override
    protected ContainerRegistry newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ContainerRegistry(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected ContainerRegistryDraft newDraftForCreate(@Nonnull String name, String resourceGroup) {
        return new ContainerRegistryDraft(name, resourceGroup, this);
    }

    @Nonnull
    @Override
    protected ContainerRegistryDraft newDraftForUpdate(@Nonnull ContainerRegistry origin) {
        return new ContainerRegistryDraft(origin);
    }

    @Override
    public Registries getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ContainerRegistryManager::containerRegistries).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container Registry";
    }
}
