/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.models.SqlDatabase;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class MicrosoftSqlDatabase extends AbstractAzResource<MicrosoftSqlDatabase, MicrosoftSqlServer, SqlDatabase> implements IDatabase {

    protected MicrosoftSqlDatabase(@Nonnull String name, @Nonnull MicrosoftSqlDatabaseModule module) {
        super(name, module);
    }

    protected MicrosoftSqlDatabase(@Nonnull SqlDatabase remote, @Nonnull MicrosoftSqlDatabaseModule module) {
        super(remote.name(), module);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, MicrosoftSqlDatabase, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SqlDatabase remote) {
        return Status.UNKNOWN;
    }

    @Nonnull
    @Override
    public MicrosoftSqlServer getServer() {
        return this.getParent();
    }

    @Nullable
    @Override
    public String getCollation() {
        return this.remoteOptional().map(SqlDatabase::collation).orElse(null);
    }

    @Nullable
    public OffsetDateTime getCreationDate() {
        return this.remoteOptional().map(SqlDatabase::creationDate).orElse(null);
    }

    @Nonnull
    public JdbcUrl getJdbcUrl() {
        return JdbcUrl.sqlserver(this.getParent().getFullyQualifiedDomainName(), this.getName());
    }
}
