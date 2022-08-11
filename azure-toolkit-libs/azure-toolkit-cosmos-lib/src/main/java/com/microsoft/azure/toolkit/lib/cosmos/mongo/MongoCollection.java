/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.cosmos.fluent.models.MongoDBCollectionGetResultsInner;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class MongoCollection extends AbstractAzResource<MongoCollection, MongoDatabase, MongoDBCollectionGetResultsInner> implements Deletable, ICosmosCollection {

    protected MongoCollection(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MongoCollectionModule module) {
        super(name, resourceGroupName, module);
    }

    protected MongoCollection(@Nonnull MongoDBCollectionGetResultsInner remote, @Nonnull MongoCollectionModule module) {
        super(remote.name(), module);
        this.setRemote(remote);
    }

    protected MongoCollection(@Nonnull MongoCollection collection) {
        super(collection);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, MongoCollection, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull MongoDBCollectionGetResultsInner remote) {
        return Status.RUNNING;
    }
}
