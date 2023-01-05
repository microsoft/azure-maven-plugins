/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.core.http.rest.Page;
import com.azure.resourcemanager.mysqlflexibleserver.MySqlManager;
import com.azure.resourcemanager.mysqlflexibleserver.models.Database;
import com.azure.resourcemanager.mysqlflexibleserver.models.Databases;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class MySqlDatabaseModule extends AbstractAzResourceModule<MySqlDatabase, MySqlServer, Database> {
    public static final String NAME = "databases";

    public MySqlDatabaseModule(@Nonnull MySqlServer parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected MySqlDatabase newResource(@Nonnull Database database) {
        return new MySqlDatabase(database, this);
    }

    @Nonnull
    @Override
    protected MySqlDatabase newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MySqlDatabase(name, this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends Page<Database>> loadResourcePagesFromAzure() {
        final MySqlServer p = this.getParent();
        return Optional.ofNullable(getClient())
            .map(c -> c.listByServer(p.getResourceGroupName(), p.getName()).iterableByPage(PAGE_SIZE).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"})
    protected Stream<Database> loadResourcesFromAzure() {
        final MySqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.listByServer(p.getResourceGroupName(), p.getName()).stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected Database loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        final MySqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.get(p.getResourceGroupName(), p.getName(), name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/mysql.delete_database.database", params = {"nameFromResourceId(id)"})
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final MySqlServer p = this.getParent();
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.delete(p.getResourceGroupName(), p.getName(), name));
    }

    @Nullable
    @Override
    protected Databases getClient() {
        return Optional.ofNullable(this.getParent().getParent().getRemote()).map(MySqlManager::databases).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "MySQL database";
    }
}
