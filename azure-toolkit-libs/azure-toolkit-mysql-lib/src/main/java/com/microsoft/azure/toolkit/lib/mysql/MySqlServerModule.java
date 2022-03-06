/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.Server;
import com.azure.resourcemanager.mysql.models.Servers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class MySqlServerModule extends AbstractAzResourceModule<MySqlServer, MySqlResourceManager, Server> {

    public static final String NAME = "servers";

    public MySqlServerModule(@Nonnull MySqlResourceManager parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected Stream<Server> loadResourcesFromAzure() {
        log.debug("[{}]:loadResourcesFromAzure()", this.getName());
        return this.getClient().list().stream();
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected Server loadResourceFromAzure(@Nonnull String name, @Nonnull String resourceGroup) {
        return this.getClient().getByResourceGroup(resourceGroup, name);
    }

    @Override
    @AzureOperation(
        name = "resource.delete_resource.resource|type",
        params = {"nameFromResourceId(resourceId)", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        this.getClient().deleteById(resourceId);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected MySqlServerDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "resource group is required.";
        return new MySqlServerDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected MySqlServerDraft newDraftForUpdate(@Nonnull MySqlServer origin) {
        return new MySqlServerDraft(origin);
    }

    @Override
    public Servers getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(MySqlManager::servers).orElse(null);
    }

    @Nonnull
    protected MySqlServer newResource(@Nonnull Server r) {
        return new MySqlServer(r, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "MySQL server";
    }
}
