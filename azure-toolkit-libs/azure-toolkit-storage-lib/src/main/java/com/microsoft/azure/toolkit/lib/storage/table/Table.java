/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.table;

import com.azure.data.tables.TableClient;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractEmulatableAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class Table extends AbstractEmulatableAzResource<Table, StorageAccount, TableClient>
    implements Deletable {

    protected Table(@Nonnull String name, @Nonnull TableModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    public Table(@Nonnull Table origin) {
        super(origin);
    }

    protected Table(@Nonnull TableClient remote, @Nonnull TableModule module) {
        super(remote.getTableName(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull TableClient remote) {
        return "";
    }
}
