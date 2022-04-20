/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.azure.resourcemanager.sql.models.SqlServers;
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
public class MicrosoftSqlServerModule extends AbstractAzResourceModule<MicrosoftSqlServer, MicrosoftSqlServiceSubscription, SqlServer> {

    public static final String NAME = "servers";

    public MicrosoftSqlServerModule(@Nonnull MicrosoftSqlServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected Stream<SqlServer> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected SqlServer loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(c -> c.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(
        name = "resource.delete_resource.resource|type",
        params = {"nameFromResourceId(resourceId)", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.deleteById(resourceId));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected MicrosoftSqlServerDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new MicrosoftSqlServerDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected MicrosoftSqlServerDraft newDraftForUpdate(@Nonnull MicrosoftSqlServer server) {
        return new MicrosoftSqlServerDraft(server);
    }

    @Override
    public SqlServers getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SqlServerManager::sqlServers).orElse(null);
    }

    @Nonnull
    protected MicrosoftSqlServer newResource(@Nonnull SqlServer r) {
        return new MicrosoftSqlServer(r, this);
    }

    @Nonnull
    protected MicrosoftSqlServer newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MicrosoftSqlServer(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SQL server";
    }
}
