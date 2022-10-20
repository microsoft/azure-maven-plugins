/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.table;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TableDraft extends Table implements AzResource.Draft<Table, TableClient> {
    @Getter
    @Nullable
    private final Table origin;

    TableDraft(@Nonnull String name, @Nonnull TableModule module) {
        super(name, module);
        this.origin = null;
    }

    TableDraft(@Nonnull Table origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Nonnull
    @Override
    public TableClient createResourceInAzure() {
        final TableModule module = (TableModule) this.getModule();
        final TableServiceClient client = module.getTableServiceClient();
        return client.createTable(this.getName());
    }

    @Nonnull
    @Override
    public TableClient updateResourceInAzure(@Nonnull TableClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
