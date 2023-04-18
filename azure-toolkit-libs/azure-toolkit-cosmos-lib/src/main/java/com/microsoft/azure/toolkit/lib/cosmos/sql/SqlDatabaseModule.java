/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cosmos.fluent.SqlResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.SqlDatabaseGetResultsInner;
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
import java.util.stream.Stream;

public class SqlDatabaseModule extends AbstractAzResourceModule<SqlDatabase, CosmosDBAccount, SqlDatabaseGetResultsInner> {
    private static final String NAME = "sqlDatabases";

    public SqlDatabaseModule(@Nonnull SqlCosmosDBAccount parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SQL Database";
    }

    @Nonnull
    @Override
    protected SqlDatabase newResource(@Nonnull SqlDatabaseGetResultsInner resource) {
        return new SqlDatabase(resource, this);
    }

    @Nonnull
    @Override
    protected SqlDatabase newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SqlDatabase(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, SqlDatabaseGetResultsInner>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listSqlDatabases(parent.getResourceGroupName(), parent.getName()).iterableByPage(getPageSize()).iterator();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    protected Stream<SqlDatabaseGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listSqlDatabases(parent.getResourceGroupName(), parent.getName()).stream();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected SqlDatabaseGetResultsInner loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
                try {
                    return client.getSqlDatabase(parent.getResourceGroupName(), parent.getName(), name);
                } catch (RuntimeException e) {
                    return null;
                }
            }
        ).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_sql_database.database", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteSqlDatabase(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @Nonnull
    @Override
    protected SqlDatabaseDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new SqlDatabaseDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nonnull
    @Override
    protected SqlDatabaseDraft newDraftForUpdate(@Nonnull SqlDatabase sqlDatabase) {
        throw new UnsupportedOperationException("not support");
    }

    @Nullable
    @Override
    protected SqlResourcesClient getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(account -> account.manager().serviceClient().getSqlResources()).orElse(null);
    }
}
