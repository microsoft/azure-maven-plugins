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
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class MicrosoftSqlServerModule extends AbstractAzResourceModule<MicrosoftSqlServer, MicrosoftSqlResourceManager, SqlServer> {

    public static final String NAME = "servers";

    public MicrosoftSqlServerModule(@Nonnull MicrosoftSqlResourceManager parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Stream<SqlServer> loadResourcesFromAzure() {
        return this.getClient().list().stream();
    }

    @Nullable
    @Override
    protected SqlServer loadResourceFromAzure(@Nonnull String name, @Nonnull String resourceGroup) {
        return this.getClient().getByResourceGroup(resourceGroup, name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        this.getClient().deleteById(resourceId);
    }

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected MicrosoftSqlServerDraft newDraftForCreate(@Nonnull String name, @Nonnull String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new MicrosoftSqlServerDraft(name, resourceGroupName, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected MicrosoftSqlServerDraft newDraftForUpdate(@Nonnull MicrosoftSqlServer server) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
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

    @Override
    public String getResourceTypeName() {
        return "SQL server firewall rule";
    }
}
