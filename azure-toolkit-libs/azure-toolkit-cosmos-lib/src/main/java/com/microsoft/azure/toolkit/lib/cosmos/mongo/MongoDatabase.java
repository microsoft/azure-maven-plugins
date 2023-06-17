/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.cosmos.fluent.models.MongoDBDatabaseGetResultsInner;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabase;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class MongoDatabase extends AbstractAzResource<MongoDatabase, CosmosDBAccount, MongoDBDatabaseGetResultsInner> implements Deletable, ICosmosDatabase {

    private final MongoCollectionModule collectionModule;

    protected MongoDatabase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MongoDatabaseModule module) {
        super(name, resourceGroupName, module);
        this.collectionModule = new MongoCollectionModule(this);
    }

    protected MongoDatabase(@Nonnull MongoDatabase account) {
        super(account);
        this.collectionModule = new MongoCollectionModule(this);
    }

    protected MongoDatabase(@Nonnull MongoDBDatabaseGetResultsInner remote, @Nonnull MongoDatabaseModule module) {
        super(remote.name(), module);
        this.collectionModule = new MongoCollectionModule(this);
    }

    public Region getRegion() {
        return getParent().getRegion();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(collectionModule);
    }

    public MongoCollectionModule collections() {
        return this.collectionModule;
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull MongoDBDatabaseGetResultsInner remote) {
        return Status.RUNNING;
    }

    @Override
    public List<? extends ICosmosCollection> listCollection() {
        return this.collections().list();
    }
}
