/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cosmos.fluent.MongoDBResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.MongoDBDatabaseGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MongoDatabaseModule extends AbstractAzResourceModule<MongoDatabase, CosmosDBAccount, MongoDBDatabaseGetResultsInner> {
    private static final String NAME = "mongodbDatabases";

    public MongoDatabaseModule(@Nonnull MongoCosmosDBAccount parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "MongoDB Database";
    }

    @Nonnull
    @Override
    protected MongoDatabase newResource(@Nonnull MongoDBDatabaseGetResultsInner resource) {
        return new MongoDatabase(resource, this);
    }

    @Nonnull
    @Override
    protected MongoDatabase newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MongoDatabase(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, MongoDBDatabaseGetResultsInner>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listMongoDBDatabases(parent.getResourceGroupName(), parent.getName()).iterableByPage(getPageSize()).iterator();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    protected Stream<MongoDBDatabaseGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listMongoDBDatabases(parent.getResourceGroupName(), parent.getName()).stream();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected MongoDBDatabaseGetResultsInner loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.getMongoDBDatabase(parent.getResourceGroupName(), parent.getName(), name);
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(null);
    }

    @Nonnull
    @Override
    protected MongoDatabaseDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new MongoDatabaseDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nonnull
    @Override
    protected MongoDatabaseDraft newDraftForUpdate(@Nonnull MongoDatabase mongoDatabase) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_mongo_table.table", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteMongoDBDatabase(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @Nullable
    @Override
    protected MongoDBResourcesClient getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(account -> account.manager().serviceClient().getMongoDBResources()).orElse(null);
    }
}
