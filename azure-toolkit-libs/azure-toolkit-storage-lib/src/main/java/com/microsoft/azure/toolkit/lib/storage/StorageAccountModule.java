/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class StorageAccountModule extends AbstractAzResourceModule<StorageAccount, StorageServiceSubscription, com.azure.resourcemanager.storage.models.StorageAccount> {

    public static final String NAME = "storageAccounts";

    public StorageAccountModule(@Nonnull StorageServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    public StorageAccounts getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(StorageManager::storageAccounts).orElse(null);
    }

    @Nonnull
    @Override
    protected StorageAccountDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new StorageAccountDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected StorageAccountDraft newDraftForUpdate(@Nonnull StorageAccount origin) {
        return new StorageAccountDraft(origin);
    }

    @Nonnull
    protected StorageAccount newResource(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount r) {
        return new StorageAccount(r, this);
    }

    @Nonnull
    protected StorageAccount newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new StorageAccount(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure Storage Account";
    }
}
