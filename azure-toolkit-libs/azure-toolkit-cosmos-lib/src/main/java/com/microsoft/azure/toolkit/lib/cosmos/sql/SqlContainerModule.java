/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cosmos.fluent.SqlResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.SqlContainerGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class SqlContainerModule extends AbstractAzResourceModule<SqlContainer, SqlDatabase, SqlContainerGetResultsInner> {
    private static final String NAME = "containers";

    public SqlContainerModule(@NotNull SqlDatabase parent) {
        super(NAME, parent);
    }

    @NotNull
    @Override
    protected SqlContainer newResource(@NotNull SqlContainerGetResultsInner sqlContainer) {
        return new SqlContainer(sqlContainer, this);
    }

    @NotNull
    @Override
    protected SqlContainer newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new SqlContainer(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, SqlContainerGetResultsInner>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listSqlContainers(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName()).iterableByPage(PAGE_SIZE).iterator();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Collections.emptyIterator());
    }

    @NotNull
    @Override
    protected Stream<SqlContainerGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listSqlContainers(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName()).stream();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected SqlContainerGetResultsInner loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.getSqlContainer(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName(), name);
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_sql_container.container", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteSqlContainer(id.resourceGroupName(), id.parent().parent().name(), id.parent().name(), id.name()));
    }

    @NotNull
    @Override
    protected AzResource.Draft<SqlContainer, SqlContainerGetResultsInner> newDraftForCreate(@NotNull String name, @Nullable String rgName) {
        return new SqlContainerDraft(name, Objects.requireNonNull(rgName), this);
    }

    @NotNull
    @Override
    protected AzResource.Draft<SqlContainer, SqlContainerGetResultsInner> newDraftForUpdate(@NotNull SqlContainer sqlContainer) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    protected @Nullable SqlResourcesClient getClient() {
        return ((SqlDatabaseModule) this.parent.getModule()).getClient();
    }
}
