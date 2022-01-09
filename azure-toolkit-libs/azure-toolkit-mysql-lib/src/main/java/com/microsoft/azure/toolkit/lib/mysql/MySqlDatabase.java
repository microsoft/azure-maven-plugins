/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.models.Database;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class MySqlDatabase extends AbstractAzResource<MySqlDatabase, MySqlServer, Database> {

    protected MySqlDatabase(Database database, MySqlDatabaseModule module) {
        this(database.name(), module.getParent().getResourceGroupName(), module);
    }

    protected MySqlDatabase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MySqlDatabaseModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    protected void refreshRemote() {
        this.remoteOptional().ifPresent(Database::refresh);
    }

    @Override
    public List<AzResourceModule<?, MySqlDatabase, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Database remote) {
        return Status.UNKNOWN;
    }

    public String getCollation() {
        return this.remoteOptional().map(Database::collation).orElse(null);
    }

    public String getCharset() {
        return this.remoteOptional().map(Database::charset).orElse(null);
    }
}
