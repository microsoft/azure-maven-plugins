/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cosmos.fluent.MongoDBResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.MongoDBCollectionGetResultsInner;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MongoCollectionModule extends AbstractAzResourceModule<MongoCollection, MongoDatabase, MongoDBCollectionGetResultsInner> {
    private static final String NAME = "collections";

    public MongoCollectionModule(@NotNull MongoDatabase parent) {
        super(NAME, parent);
    }

    @NotNull
    @Override
    protected MongoCollection newResource(@NotNull MongoDBCollectionGetResultsInner mongoDBCollectionGetResultsInner) {
        return new MongoCollection(mongoDBCollectionGetResultsInner, this);
    }

    @NotNull
    @Override
    protected MongoCollection newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new MongoCollection(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, MongoDBCollectionGetResultsInner>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listMongoDBCollections(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName()).iterableByPage(PAGE_SIZE).iterator();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Collections.emptyIterator());
    }

    @NotNull
    @Override
    protected Stream<MongoDBCollectionGetResultsInner> loadResourcesFromAzure() {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.listMongoDBCollections(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName()).stream();
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected MongoDBCollectionGetResultsInner loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client -> {
            try {
                return client.getMongoDBCollection(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName(), name);
            } catch (final RuntimeException e) {
                return null;
            }
        }).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_mongo_collection.collection", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteMongoDBCollection(id.resourceGroupName(), id.parent().parent().name(), id.parent().name(), id.name()));
    }

    @NotNull
    @Override
    protected AzResource.Draft<MongoCollection, MongoDBCollectionGetResultsInner> newDraftForCreate(@NotNull String name, @Nullable String rgName) {
        return new MongoCollectionDraft(name, Objects.requireNonNull(rgName), this);
    }

    @NotNull
    @Override
    protected AzResource.Draft<MongoCollection, MongoDBCollectionGetResultsInner> newDraftForUpdate(@NotNull MongoCollection mongoCollection) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nullable
    @Override
    protected MongoDBResourcesClient getClient() {
        return ((MongoDatabaseModule) this.parent.getModule()).getClient();
    }
}
