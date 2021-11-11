/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Database;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlDatabaseEntity;

import javax.annotation.Nonnull;

public class PostgreSqlDatabase extends AbstractAzureResource<PostgreSqlDatabase, PostgreSqlDatabaseEntity, Database> implements IDatabase {
    @Nonnull
    private final PostgreSqlManager manager;

    protected PostgreSqlDatabase(PostgreSqlManager manager, @Nonnull PostgreSqlDatabaseEntity entity) {
        super(entity);
        this.manager = manager;
    }

    @Override
    protected Database loadRemote() {
        return remote();
    }
}
