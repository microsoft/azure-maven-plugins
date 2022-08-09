/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.resourcemanager.cosmos.fluent.CassandraResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.CassandraKeyspaceGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

public class CassandraKeyspaceModule extends AbstractAzResourceModule<CassandraKeyspace, CosmosDBAccount, CassandraKeyspaceGetResultsInner> {
    private static final String NAME = "cassandraKeyspaces";

    public CassandraKeyspaceModule(@NotNull CassandraCosmosDBAccount parent) {
        super(NAME, parent);
    }

    @NotNull
    @Override
    protected CassandraKeyspace newResource(@NotNull CassandraKeyspaceGetResultsInner resource) {
        return new CassandraKeyspace(resource, this);
    }

    @NotNull
    @Override
    protected CassandraKeyspace newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new CassandraKeyspace(name, resourceGroupName, this);
    }

    @NotNull
    @Override
    protected Stream<CassandraKeyspaceGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client ->
                client.listCassandraKeyspaces(parent.getResourceGroupName(), parent.getName()).stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected CassandraKeyspaceGetResultsInner loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> client.getCassandraKeyspace(parent.getResourceGroupName(), parent.getName(), name)).orElse(null);
    }

    @Override
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteCassandraKeyspace(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @Override
    protected CassandraResourcesClient getClient() {
        return this.parent.getRemote().manager().serviceClient().getCassandraResources();
    }
}
