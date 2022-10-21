/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.table;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class TableModule extends AbstractAzResourceModule<Table, StorageAccount, TableClient> {

    public static final String NAME = "tables";
    private TableServiceClient client;

    public TableModule(@Nonnull StorageAccount parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    synchronized TableServiceClient getTableServiceClient() {
        if (Objects.isNull(this.client)) {
            final String connectionString = this.parent.getConnectionString();
            this.client = new TableServiceClientBuilder().connectionString(connectionString).buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Stream<TableClient> loadResourcesFromAzure() {
        final TableServiceClient client = this.getTableServiceClient();
        return client.listTables().stream().map(s -> client.getTableClient(s.getName()));
    }

    @Nullable
    @Override
    protected TableClient loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return this.loadResourcesFromAzure().filter(c -> c.getTableName().equals(name)).findAny().orElse(null);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final TableServiceClient client = this.getTableServiceClient();
        client.deleteTable(id.name());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected TableDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new TableDraft(name, this);
    }

    @Nonnull
    protected Table newResource(@Nonnull TableClient r) {
        return new Table(r, this);
    }

    @Nonnull
    protected Table newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Table(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Table";
    }
}
