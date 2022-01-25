/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.Servers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class PostgreSqlServerModule extends AbstractAzResourceModule<PostgreSqlServer, PostgreSqlResourceManager, Server> {

    public static final String NAME = "servers";

    public PostgreSqlServerModule(@Nonnull PostgreSqlResourceManager parent) {
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
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected PostgreSqlServerDraft newDraftForCreate(@Nonnull String name, @Nonnull String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new PostgreSqlServerDraft(name, resourceGroupName, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected PostgreSqlServerDraft newDraftForUpdate(@Nonnull PostgreSqlServer origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new PostgreSqlServerDraft(origin);
    }

    @Override
    public Servers getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(PostgreSqlManager::servers).orElse(null);
    }

    @Nonnull
    protected PostgreSqlServer newResource(@Nonnull Server r) {
        return new PostgreSqlServer(r, this);
    }

    @Override
    public String getResourceTypeName() {
        return "PostgreSQL server";
    }
}
