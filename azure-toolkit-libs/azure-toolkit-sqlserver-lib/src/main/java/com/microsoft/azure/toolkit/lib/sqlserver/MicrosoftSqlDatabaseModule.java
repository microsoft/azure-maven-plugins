/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.models.SqlDatabase;
import com.azure.resourcemanager.sql.models.SqlDatabaseOperations;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class MicrosoftSqlDatabaseModule extends AbstractAzResourceModule<MicrosoftSqlDatabase, MicrosoftSqlServer, SqlDatabase> {
    public static final String NAME = "databases";

    public MicrosoftSqlDatabaseModule(@Nonnull MicrosoftSqlServer parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlDatabase newResource(@Nonnull SqlDatabase database) {
        return new MicrosoftSqlDatabase(database, this);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlDatabase newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MicrosoftSqlDatabase(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<SqlDatabase> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected SqlDatabase loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(this.getClient()).map(c -> c.get(name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "sqlserver.delete_database.database", params = {"nameFromResourceId(id)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.delete(name));
    }

    @Nullable
    @Override
    protected SqlDatabaseOperations.SqlDatabaseActionsDefinition getClient() {
        return Optional.ofNullable(this.getParent().getRemote()).map(SqlServer::databases).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SQL server database";
    }
}
