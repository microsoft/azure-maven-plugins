/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.resourcemanager.cosmos.fluent.models.CassandraTableGetResultsInner;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class CassandraTable extends AbstractAzResource<CassandraTable, CassandraKeyspace, CassandraTableGetResultsInner> implements Deletable {

    protected CassandraTable(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CassandraTableModule module) {
        super(name, resourceGroupName, module);
    }

    protected CassandraTable(@Nonnull CassandraTableGetResultsInner remote, @Nonnull CassandraTableModule module) {
        super(remote.name(), module);
        this.setRemote(remote);
    }

    protected CassandraTable(@Nonnull CassandraTable collection) {
        super(collection);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, CassandraTable, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull CassandraTableGetResultsInner remote) {
        return Status.RUNNING;
    }
}
