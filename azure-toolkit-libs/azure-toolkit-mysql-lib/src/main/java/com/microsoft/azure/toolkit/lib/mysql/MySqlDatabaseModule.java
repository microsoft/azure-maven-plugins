/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.Database;
import com.azure.resourcemanager.mysql.models.Databases;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class MySqlDatabaseModule extends AbstractAzResourceModule<MySqlDatabase, MySqlServer, Database> {
    public static final String NAME = "databases";

    public MySqlDatabaseModule(@Nonnull MySqlServer parent) {
        super(NAME, parent);
    }

    @Override
    protected MySqlDatabase newResource(@Nonnull Database database) {
        return new MySqlDatabase(database, this);
    }

    @Nonnull
    @Override
    protected Stream<Database> loadResourcesFromAzure() {
        return this.getClient().listByServer(this.getParent().getResourceGroupName(), this.getParent().getName()).stream();
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
        return Optional.ofNullable(this.getParent().getParent().getRemote()).map(MySqlManager::databases).orElse(null);
    }

    @Override
    public String getResourceTypeName() {
        return "MySQL database";
    }
}
