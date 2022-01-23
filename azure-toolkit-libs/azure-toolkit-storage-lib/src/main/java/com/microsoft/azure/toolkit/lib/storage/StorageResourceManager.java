/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.CheckNameAvailabilityResult;
import com.azure.resourcemanager.storage.models.Reason;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class StorageResourceManager extends AbstractAzResourceManager<StorageResourceManager, StorageManager> {
    @Nonnull
    private final String subscriptionId;
    private final StorageAccountModule storageModule;

    StorageResourceManager(@Nonnull String subscriptionId, AzureStorageAccount service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.storageModule = new StorageAccountModule(this);
    }

    StorageResourceManager(@Nonnull StorageManager remote, AzureStorageAccount service) {
        this(remote.subscriptionId(), service);
    }

    @Override
    public List<AzResourceModule<?, StorageResourceManager, ?>> getSubModules() {
        return Collections.singletonList(storageModule);
    }

    public StorageAccountModule storageAccounts() {
        return this.storageModule;
    }

    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.storageModule.getName());
    }

    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    @AzureOperation(name = "storage.check_name.name", params = {"name"}, type = AzureOperation.Type.SERVICE)
    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String name) {
        CheckNameAvailabilityResult result = Objects.requireNonNull(this.getRemote()).storageAccounts().checkNameAvailability(name);
        return new CheckNameAvailabilityResultEntity(result.isAvailable(),
            Optional.ofNullable(result.reason()).map(Reason::toString).orElse(null), result.message());
    }
}

