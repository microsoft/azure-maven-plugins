/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.resourcemanager.cosmos.fluent.CassandraResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.CassandraKeyspaceGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CassandraKeyspaceModule extends AbstractAzResourceModule<CassandraKeyspace, CosmosDBAccount, CassandraKeyspaceGetResultsInner> {
    private static final String NAME = "cassandraKeyspaces";

    public CassandraKeyspaceModule(@NotNull CassandraCosmosDBAccount parent) {
        super(NAME, parent);
    }

    @NotNull
    @Override
    public String getResourceTypeName() {
        return "Cassandra Keyspace";
    }

    @NotNull
    @Override
    protected CassandraKeyspace newResource(@NotNull CassandraKeyspaceGetResultsInner resource) {
        return new CassandraKeyspace(resource, this);
    }

    @NotNull
    @Override
    protected CassandraKeyspace newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new CassandraKeyspace(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @NotNull
    @Override
    protected Stream<CassandraKeyspaceGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listCassandraKeyspaces(parent.getResourceGroupName(), parent.getName()).stream();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected CassandraKeyspaceGetResultsInner loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.getCassandraKeyspace(parent.getResourceGroupName(), parent.getName(), name);
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_cassandra_keyspace.keyspace", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteCassandraKeyspace(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @NotNull
    @Override
    protected CassandraKeyspaceDraft newDraftForCreate(@NotNull String name, @Nullable String rgName) {
        return new CassandraKeyspaceDraft(name, Objects.requireNonNull(rgName), this);
    }

    @NotNull
    @Override
    protected CassandraKeyspaceDraft newDraftForUpdate(@NotNull CassandraKeyspace cassandraKeyspace) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected CassandraResourcesClient getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(account -> account.manager().serviceClient().getCassandraResources()).orElse(null);
    }
}
