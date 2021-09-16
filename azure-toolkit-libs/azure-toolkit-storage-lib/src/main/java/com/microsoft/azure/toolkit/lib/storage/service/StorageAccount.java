/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

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

    @AzureOperation(name = "storage|account.get_connection_string", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public String getConnectionString() {
        // refer https://github.com/Azure/azure-cli/blob/ac3b190d4d/src/azure-cli/azure/cli/command_modules/storage/operations/account.py#L232
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        return String.format("DefaultEndpointsProtocol=https;EndpointSuffix=%s;AccountName=%s;AccountKey=%s",
                suffix, this.name(), getKey());
    }

    @AzureOperation(name = "storage|account.get_key", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public String getKey() {
        final List<StorageAccountKey> keys = Objects.requireNonNull(this.remote()).getKeys();
        return StringUtils.equalsIgnoreCase(keys.get(0).keyName(), "primary") ? keys.get(0).value() : keys.get(1).value();
    }

    @Override
    public void remove() {
        this.delete();
    }
}
