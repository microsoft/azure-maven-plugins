/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

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

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected StorageAccountDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new StorageAccountDraft(name, resourceGroupName, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected StorageAccountDraft newDraftForUpdate(@Nonnull StorageAccount origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new StorageAccountDraft(origin);
    }

    @Nonnull
    protected StorageAccount newResource(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount r) {
        return new StorageAccount(r, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Azure Storage Account";
    }
}
