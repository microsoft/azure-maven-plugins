/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.storage.StorageManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountEntity;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;

public class StorageAccount extends AbstractAzureResource<StorageAccount, StorageAccountEntity, com.azure.resourcemanager.storage.models.StorageAccount>
        implements Removable, AzureOperationEvent.Source<StorageAccount>, IAzureResource<StorageAccountEntity> {
    @Nonnull
    private final StorageManager manager;

    public StorageAccount(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount server) {
        super(new StorageAccountEntity(server));
        this.manager = server.manager();
    }

    @Override
    protected com.azure.resourcemanager.storage.models.StorageAccount loadRemote() {
        try {
            this.entity().setRemote(manager.storageAccounts().getById(this.entity.getId()));
        } catch (ManagementException ex) {
            if (HttpStatus.SC_NOT_FOUND == ex.getResponse().getStatusCode()) {
                this.entity().setRemote(null);
            } else {
                throw ex;
            }
        }
        return entity.getRemote();
    }

    @AzureOperation(name = "storage|account.delete", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        if (this.exists()) {
            this.status(Status.PENDING);
            manager.storageAccounts().deleteById(this.entity.getId());
            Azure.az(AzureStorageAccount.class).refresh();
        }
    }

    @Override
    public void remove() {
        this.delete();
    }
}
