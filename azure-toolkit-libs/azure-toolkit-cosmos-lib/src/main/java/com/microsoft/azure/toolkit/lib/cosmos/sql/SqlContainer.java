/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.resourcemanager.cosmos.fluent.models.SqlContainerGetResultsInner;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class SqlContainer extends AbstractAzResource<SqlContainer, SqlDatabase, SqlContainerGetResultsInner> implements Deletable, ICosmosCollection {

    protected SqlContainer(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull SqlContainerModule module) {
        super(name, resourceGroupName, module);
    }

    protected SqlContainer(@Nonnull SqlContainerGetResultsInner remote, @Nonnull SqlContainerModule module) {
        super(remote.name(), module);
    }

    protected SqlContainer(@Nonnull SqlContainer collection) {
        super(collection);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull SqlContainerGetResultsInner remote) {
        return Status.RUNNING;
    }
}
