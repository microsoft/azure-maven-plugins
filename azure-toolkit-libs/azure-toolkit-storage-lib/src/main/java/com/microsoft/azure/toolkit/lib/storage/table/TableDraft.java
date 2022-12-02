/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.table;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
    @AzureOperation(name = "azure/storage.create_table.table", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public TableClient createResourceInAzure() {
        final TableModule module = (TableModule) this.getModule();
        final TableServiceClient client = module.getTableServiceClient();
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Table ({0}).", this.getName()));
        final TableClient table = client.createTable(this.getName());
        messager.success(AzureString.format("Table ({0}) is successfully created.", this.getName()));
        return table;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/storage.update_table.table", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public TableClient updateResourceInAzure(@Nonnull TableClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
