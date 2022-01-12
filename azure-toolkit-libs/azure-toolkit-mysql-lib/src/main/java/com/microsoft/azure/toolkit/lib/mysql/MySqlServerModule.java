/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.Server;
import com.azure.resourcemanager.mysql.models.Servers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class MySqlServerModule extends AbstractAzResourceModule<MySqlServer, MySqlResourceManager, Server> {

    public static final String NAME = "servers";

    public MySqlServerModule(@Nonnull MySqlResourceManager parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Stream<Server> loadResourcesFromAzure() {
        return this.getClient().list().stream();
    }

    @Nullable
    @Override
    protected Server loadResourceFromAzure(@Nonnull String name, @Nonnull String resourceGroup) {
        return this.getClient().getByResourceGroup(resourceGroup, name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        this.getClient().deleteById(resourceId);
    }

    @Override
    public Servers getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(MySqlManager::servers).orElse(null);
    }

    @Nonnull
    protected MySqlServer newResource(@Nonnull Server r) {
        return new MySqlServer(r, this);
    }

    @Override
    protected MySqlServerDraft newDraft(@Nonnull String name, String resourceGroup) {
        return new MySqlServerDraft(name, resourceGroup, this);
    }
}
