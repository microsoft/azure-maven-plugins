/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cosmos.fluent.CassandraResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.CassandraKeyspaceGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class CassandraKeyspaceModule extends AbstractAzResourceModule<CassandraKeyspace, CosmosDBAccount, CassandraKeyspaceGetResultsInner> {
    private static final String NAME = "cassandraKeyspaces";

    public CassandraKeyspaceModule(@Nonnull CassandraCosmosDBAccount parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Cassandra Keyspace";
    }

    @Nonnull
    @Override
    protected CassandraKeyspace newResource(@Nonnull CassandraKeyspaceGetResultsInner resource) {
        return new CassandraKeyspace(resource, this);
    }

    @Nonnull
    @Override
    protected CassandraKeyspace newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new CassandraKeyspace(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, CassandraKeyspaceGetResultsInner>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listCassandraKeyspaces(parent.getResourceGroupName(), parent.getName()).iterableByPage(getPageSize()).iterator();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    protected CassandraKeyspaceGetResultsInner loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.getCassandraKeyspace(parent.getResourceGroupName(), parent.getName(), name);
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_cassandra_keyspace.keyspace", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteCassandraKeyspace(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @Nonnull
    @Override
    protected CassandraKeyspaceDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new CassandraKeyspaceDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nonnull
    @Override
    protected CassandraKeyspaceDraft newDraftForUpdate(@Nonnull CassandraKeyspace cassandraKeyspace) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected CassandraResourcesClient getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(account -> account.manager().serviceClient().getCassandraResources()).orElse(null);
    }
}
