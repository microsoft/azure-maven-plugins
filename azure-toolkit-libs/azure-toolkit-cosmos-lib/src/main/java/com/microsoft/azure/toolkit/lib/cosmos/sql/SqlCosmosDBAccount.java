/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountModule;
import org.jetbrains.annotations.NotNull;

public class SqlCosmosDBAccount extends CosmosDBAccount {

    private final SqlDatabaseModule sqlDatabaseModule;

    public SqlCosmosDBAccount(@NotNull String name, @NotNull String resourceGroupName, @NotNull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
        this.sqlDatabaseModule = new SqlDatabaseModule(this);
    }

    public SqlCosmosDBAccount(@NotNull CosmosDBAccount account) {
        super(account);
        this.sqlDatabaseModule = new SqlDatabaseModule(this);
    }

    public SqlCosmosDBAccount(@NotNull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote, @NotNull CosmosDBAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.sqlDatabaseModule = new SqlDatabaseModule(this);
        this.setRemote(remote);
    }

    public SqlDatabaseModule sqlDatabases() {
        return this.sqlDatabaseModule;
    }
}
