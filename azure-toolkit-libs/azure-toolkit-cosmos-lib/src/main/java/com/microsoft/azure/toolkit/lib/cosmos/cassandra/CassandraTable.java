/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.resourcemanager.cosmos.fluent.models.CassandraTableGetResultsInner;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class CassandraTable extends AbstractAzResource<CassandraTable, CassandraKeyspace, CassandraTableGetResultsInner> implements Deletable, ICosmosCollection {

    protected CassandraTable(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CassandraTableModule module) {
        super(name, resourceGroupName, module);
    }

    protected CassandraTable(@Nonnull CassandraTableGetResultsInner remote, @Nonnull CassandraTableModule module) {
        super(remote.name(), module);
    }

    protected CassandraTable(@Nonnull CassandraTable collection) {
        super(collection);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull CassandraTableGetResultsInner remote) {
        return Status.RUNNING;
    }
}
