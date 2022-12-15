/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysqlflexibleserver.MySqlManager;
import com.azure.resourcemanager.mysqlflexibleserver.models.Server;
import com.azure.resourcemanager.mysqlflexibleserver.models.Servers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class MySqlServerModule extends AbstractAzResourceModule<MySqlServer, MySqlServiceSubscription, Server> {

    public static final String NAME = "flexibleServers";

    public MySqlServerModule(@Nonnull MySqlServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<Server> loadResourcesFromAzure() {
        log.debug("[{}]:loadResourcesFromAzure()", this.getName());
        return Optional.ofNullable(this.getClient()).map(c -> c.list().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource_in_azure.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Server loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(c -> c.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "mysql.delete_server_in_azure.server", params = {"nameFromResourceId(id)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected MySqlServerDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new MySqlServerDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected MySqlServerDraft newDraftForUpdate(@Nonnull MySqlServer origin) {
        return new MySqlServerDraft(origin);
    }

    @Nullable
    @Override
    public Servers getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(MySqlManager::servers).orElse(null);
    }

    @Nonnull
    protected MySqlServer newResource(@Nonnull Server r) {
        return new MySqlServer(r, this);
    }

    @Nonnull
    protected MySqlServer newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MySqlServer(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "MySQL server";
    }
}
