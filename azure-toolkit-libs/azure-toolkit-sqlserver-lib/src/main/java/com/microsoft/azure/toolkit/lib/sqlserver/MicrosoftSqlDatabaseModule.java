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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class MicrosoftSqlDatabaseModule extends AbstractAzResourceModule<MicrosoftSqlDatabase, MicrosoftSqlServer, SqlDatabase> {
    public static final String NAME = "databases";

    public MicrosoftSqlDatabaseModule(@Nonnull MicrosoftSqlServer parent) {
        super(NAME, parent);
    }

    @Override
    protected MicrosoftSqlDatabase newResource(@Nonnull SqlDatabase database) {
        return new MicrosoftSqlDatabase(database, this);
    }

    @Nonnull
    @Override
    protected Stream<SqlDatabase> loadResourcesFromAzure() {
        return this.getClient().list().stream();
    }

    @Nullable
    @Override
    protected SqlDatabase loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        return this.getClient().get(name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        this.getClient().delete(name);
    }

    @Override
    protected SqlDatabaseOperations.SqlDatabaseActionsDefinition getClient() {
        return Optional.ofNullable(this.getParent().getRemote()).map(SqlServer::databases).orElse(null);
    }

    @Override
    public String getResourceTypeName() {
        return "SQL server database";
    }
}
