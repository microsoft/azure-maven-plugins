/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.resourcemanager.containerregistry.ContainerRegistryManager;
import com.azure.resourcemanager.containerregistry.models.Registries;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class AzureContainerRegistryModule extends AbstractAzResourceModule<ContainerRegistry, AzureContainerRegistryResourceManager, Registry> {

    public static final String NAME = "registries";

    public AzureContainerRegistryModule(@NotNull AzureContainerRegistryResourceManager parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Stream<Registry> loadResourcesFromAzure() {
        return this.getClient().list().stream();
    }

    @Nullable
    @Override
    protected Registry loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        return this.getClient().getByResourceGroup(resourceGroup, name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        this.getClient().deleteById(resourceId);
    }

    @Override
    protected ContainerRegistry newResource(@NotNull Registry registry) {
        return new ContainerRegistry(registry, this);
    }

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ContainerRegistryDraft newDraftForCreate(@Nonnull String name, String resourceGroup) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new ContainerRegistryDraft(name, resourceGroup, this);
    }

    @Override
    @AzureOperation(
            name = "resource.draft_for_update.resource|type",
            params = {"origin.getName()", "this.getResourceTypeName()"},
            type = AzureOperation.Type.SERVICE
    )
    protected ContainerRegistryDraft newDraftForUpdate(@Nonnull ContainerRegistry registry) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new ContainerRegistryDraft(registry);
    }

    public ContainerRegistry get(@Nonnull String resourceId) {
        return this.list().stream().filter(resource -> StringUtils.equalsIgnoreCase(resourceId, resource.getId())).findFirst().orElse(null);
    }

    @Override
    public Registries getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ContainerRegistryManager::containerRegistries).orElse(null);
    }

    @Override
    public String getResourceTypeName() {
        return "Container Registry";
    }
}
