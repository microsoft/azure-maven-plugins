/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.cosmos.fluent.MongoDBResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.MongoDBDatabaseGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MongoDatabaseModule extends AbstractAzResourceModule<MongoDatabase, CosmosDBAccount, MongoDBDatabaseGetResultsInner> {
    private static final String NAME = "mongodbDatabases";

    public MongoDatabaseModule(@NotNull MongoCosmosDBAccount parent) {
        super(NAME, parent);
    }

    @NotNull
    @Override
    public String getResourceTypeName() {
        return "MongoDB Database";
    }

    @NotNull
    @Override
    protected MongoDatabase newResource(@NotNull MongoDBDatabaseGetResultsInner resource) {
        return new MongoDatabase(resource, this);
    }

    @NotNull
    @Override
    protected MongoDatabase newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new MongoDatabase(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @NotNull
    @Override
    protected Stream<MongoDBDatabaseGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client ->
                client.listMongoDBDatabases(parent.getResourceGroupName(), parent.getName()).stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected MongoDBDatabaseGetResultsInner loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.getMongoDBDatabase(parent.getResourceGroupName(), parent.getName(), name);
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(null);
    }

    @NotNull
    @Override
    protected MongoDatabaseDraft newDraftForCreate(@NotNull String name, @Nullable String rgName) {
        return new MongoDatabaseDraft(name, Objects.requireNonNull(rgName), this);
    }

    @NotNull
    @Override
    protected MongoDatabaseDraft newDraftForUpdate(@NotNull MongoDatabase mongoDatabase) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    @AzureOperation(name = "cosmos.delete_mongo_table_in_azure.table", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteMongoDBDatabase(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @Nullable
    @Override
    protected MongoDBResourcesClient getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(account -> account.manager().serviceClient().getMongoDBResources()).orElse(null);
    }
}
