/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Database;
import com.azure.resourcemanager.postgresql.models.Databases;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class PostgreSqlDatabaseModule extends AbstractAzResourceModule<PostgreSqlDatabase, PostgreSqlServer, Database> {
    public static final String NAME = "databases";

    public PostgreSqlDatabaseModule(@Nonnull PostgreSqlServer parent) {
        super(NAME, parent);
    }

    @Override
    protected PostgreSqlDatabase newResource(@Nonnull Database database) {
        return new PostgreSqlDatabase(database, this);
    }

    @Nonnull
    @Override
    protected Stream<Database> loadResourcesFromAzure() {
        // https://docs.microsoft.com/en-us/azure/postgresql/concepts-servers
        // azure_maintenance - This database is used to separate the processes that provide the managed service from user actions.
        // You do not have access to this database.
        return this.getClient().listByServer(this.getParent().getResourceGroupName(), this.getParent().getName()).stream()
            .filter(d -> !"azure_maintenance".equals(d.name()));
    }

    @Nullable
    @Override
    protected Database loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        return this.getClient().get(this.getParent().getResourceGroupName(), this.getParent().getName(), name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        this.getClient().delete(this.getParent().getResourceGroupName(), this.getParent().getName(), name);
    }

    @Override
    protected Databases getClient() {
        return Optional.ofNullable(this.getParent().getParent().getRemote()).map(PostgreSqlManager::databases).orElse(null);
    }

    @Override
    public String getResourceTypeName() {
        return "PostgreSQL database";
    }
}
