/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Database;
import com.azure.resourcemanager.postgresql.models.Databases;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class PostgreSqlDatabaseModule extends AbstractAzResourceModule<PostgreSqlDatabase, PostgreSqlServer, Database> {
    public static final String NAME = "databases";

    public PostgreSqlDatabaseModule(@Nonnull PostgreSqlServer parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected PostgreSqlDatabase newResource(@Nonnull Database database) {
        return new PostgreSqlDatabase(database, this);
    }

    @Nonnull
    @Override
    protected PostgreSqlDatabase newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new PostgreSqlDatabase(name, this);
    }

    @Override
    protected Iterator<? extends ContinuablePage<String, Database>> loadResourcePagesFromAzure() {
        final PostgreSqlServer server = this.getParent();
        return Optional.ofNullable(this.getClient())
            .map(c -> c.listByServer(server.getResourceGroupName(), server.getName())
                .streamByPage(PAGE_SIZE)
                .map(p -> new ItemPage<>(p.getValue().stream().filter(d -> !"azure_maintenance".equals(d.name()))))
                .iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"})
    protected Stream<Database> loadResourcesFromAzure() {
        // https://docs.microsoft.com/en-us/azure/postgresql/concepts-servers
        // azure_maintenance - This database is used to separate the processes that provide the managed service from user actions.
        // You do not have access to this database.
        final PostgreSqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient())
            .map(c -> c.listByServer(p.getResourceGroupName(), p.getName()).stream().filter(d -> !"azure_maintenance".equals(d.name())))
            .orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected Database loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        final PostgreSqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.get(p.getResourceGroupName(), p.getName(), name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/postgre.delete_database.database", params = {"nameFromResourceId(id)"})
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final PostgreSqlServer p = this.getParent();
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.delete(p.getResourceGroupName(), p.getName(), name));
    }

    @Nullable
    @Override
    protected Databases getClient() {
        return Optional.ofNullable(this.getParent().getParent().getRemote()).map(PostgreSqlManager::databases).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "PostgreSQL database";
    }
}
