/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Optional;

public class StorageAccountModule extends AbstractAzResourceModule<StorageAccount, StorageResourceManager, com.azure.resourcemanager.storage.models.StorageAccount> {

    public static final String NAME = "storageAccounts";

    public StorageAccountModule(@Nonnull StorageResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public StorageAccounts getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(StorageManager::storageAccounts).orElse(null);
    }

    @Nonnull
    protected StorageAccount newResource(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount r) {
        return new StorageAccount(r, this);
    }

    @Override
    protected StorageAccountDraft newDraft(@Nonnull String name, String resourceGroup) {
        return new StorageAccountDraft(name, resourceGroup, this);
    }
}
