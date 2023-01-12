/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.ContainerApps;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerAppModule extends AbstractAzResourceModule<ContainerApp, AzureContainerAppsServiceSubscription, com.azure.resourcemanager.appcontainers.models.ContainerApp> {
    public static final String NAME = "containerApps";

    public ContainerAppModule(@Nonnull AzureContainerAppsServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, com.azure.resourcemanager.appcontainers.models.ContainerApp>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"})
    protected Stream<com.azure.resourcemanager.appcontainers.models.ContainerApp> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(ContainerApps::list).map(PagedIterable::stream).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected com.azure.resourcemanager.appcontainers.models.ContainerApp loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
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
    protected ContainerAppDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        assert rgName != null : "'Resource group' is required.";
        return new ContainerAppDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    protected ContainerAppDraft newDraftForUpdate(@Nonnull ContainerApp containerApp) {
        return new ContainerAppDraft(containerApp);
    }

    @Nonnull
    @Override
    protected ContainerApp newResource(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp containerApp) {
        return new ContainerApp(containerApp, this);
    }

    @Nonnull
    @Override
    protected ContainerApp newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new ContainerApp(name, resourceGroupName, this);
    }

    public List<ContainerApp> listContainerAppsByEnvironment(@Nonnull final ContainerAppsEnvironment environment) {
        return listContainerAppsByEnvironment(environment.getId());
    }

    public List<ContainerApp> listContainerAppsByEnvironment(@Nonnull final String environment) {
        return list().stream().filter(app -> StringUtils.equalsIgnoreCase(app.getManagedEnvironmentId(), environment)).collect(Collectors.toList());
    }

    @Nullable
    @Override
    public ContainerApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ContainerAppsApiManager::containerApps).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container App";
    }
}
