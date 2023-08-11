/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.azure.resourcemanager.sql.models.SqlServers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class MicrosoftSqlServerModule extends AbstractAzResourceModule<MicrosoftSqlServer, MicrosoftSqlServiceSubscription, SqlServer> {

    public static final String NAME = "servers";

    public MicrosoftSqlServerModule(@Nonnull MicrosoftSqlServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, SqlServer>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient())
            .map(c -> c.list().iterableByPage(getPageSize()).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/sqlserver.load_server.server", params = {"name"})
    protected SqlServer loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(c -> c.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/sqlserver.delete_server.server", params = {"nameFromResourceId(id)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected MicrosoftSqlServerDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new MicrosoftSqlServerDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
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
