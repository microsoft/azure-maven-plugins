/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironments;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class ContainerAppsEnvironmentModule extends AbstractAzResourceModule<ContainerAppsEnvironment, AzureContainerAppsServiceSubscription, ManagedEnvironment> {
    public static final String NAME = "managedEnvironments";

    public ContainerAppsEnvironmentModule(@Nonnull AzureContainerAppsServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ManagedEnvironment>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"})
    protected Stream<ManagedEnvironment> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(ManagedEnvironments::list).map(PagedIterable::stream).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected ManagedEnvironment loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> client.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/resource.delete_resource.resource|type", params = {"nameFromResourceId(resourceId)", "this.getResourceTypeName()"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteByResourceGroup(id.resourceGroupName(), id.name()));
    }

    @Nonnull
    @Override
    protected ContainerAppsEnvironmentDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        assert rgName != null : "'Resource group' is required.";
        return new ContainerAppsEnvironmentDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    protected ContainerAppsEnvironmentDraft newDraftForUpdate(@Nonnull ContainerAppsEnvironment environment) {
        throw new UnsupportedOperationException("not support");
    }

    @Nonnull
    @Override
    protected ContainerAppsEnvironment newResource(@Nonnull ManagedEnvironment environment) {
        return new ContainerAppsEnvironment(environment, this);
    }

    @Nonnull
    @Override
    protected ContainerAppsEnvironment newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new ContainerAppsEnvironment(name, resourceGroupName, this);
    }

    @Nullable
    @Override
    public ManagedEnvironments getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ContainerAppsApiManager::managedEnvironments).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container Apps Environment";
    }
}
