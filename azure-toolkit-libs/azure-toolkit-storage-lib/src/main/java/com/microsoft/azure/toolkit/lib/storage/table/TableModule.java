/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.table;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AbstractEmulatableAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public class TableModule extends AbstractEmulatableAzResourceModule<Table, StorageAccount, TableClient> {

    public static final String NAME = "Azure.Table";
    private TableServiceClient client;

    public TableModule(@Nonnull StorageAccount parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    @Nullable
    synchronized TableServiceClient getTableServiceClient() {
        if (Objects.isNull(this.client) && this.parent.exists()) {
            final String connectionString = this.parent.getConnectionString();
            this.client = new TableServiceClientBuilder().addPolicy(AbstractAzServiceSubscription.getUserAgentPolicy()).connectionString(connectionString).buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, TableClient>> loadResourcePagesFromAzure() {
        if (!this.parent.exists()) {
            return Collections.emptyIterator();
        }
        final TableServiceClient client = this.getTableServiceClient();
        return Objects.requireNonNull(client).listTables().streamByPage(getPageSize())
            .map(p -> new ItemPage<>(p.getValue().stream().map(c -> client.getTableClient(c.getName()))))
            .iterator();
    }

    @Nullable
    @Override
    protected TableClient loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        if (!this.parent.exists()) {
            return null;
        }
        final TableServiceClient client = this.getTableServiceClient();
        final Stream<TableClient> resources = Objects.requireNonNull(client).listTables().stream().map(s -> client.getTableClient(s.getName()));
        return resources.filter(c -> c.getTableName().equals(name)).findAny().orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/storage.delete_table.table", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final TableServiceClient client = this.getTableServiceClient();
        Objects.requireNonNull(client).deleteTable(id.name());
    }

    @Nonnull
    @Override
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
