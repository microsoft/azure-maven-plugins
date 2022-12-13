/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.ContainerApps;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class ContainerAppModule extends AbstractAzResourceModule<ContainerApp, AzureContainerAppsServiceSubscription, com.azure.resourcemanager.appcontainers.models.ContainerApp> {
    public static final String NAME = "containerApps";

    public ContainerAppModule(@Nonnull AzureContainerAppsServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Stream<com.azure.resourcemanager.appcontainers.models.ContainerApp> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(ContainerApps::list).map(PagedIterable::stream).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appcontainers.models.ContainerApp loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> client.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    public void delete(@Nonnull String name, @Nullable String rgName) {
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteByResourceGroup(rgName, name));
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
        throw new UnsupportedOperationException("not support");
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

    @Nullable
    @Override
    public ContainerApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ContainerAppsApiManager::containerApps).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container Apps";
    }
}
