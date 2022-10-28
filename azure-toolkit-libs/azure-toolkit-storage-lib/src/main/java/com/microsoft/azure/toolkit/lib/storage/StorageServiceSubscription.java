/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.CheckNameAvailabilityResult;
import com.azure.resourcemanager.storage.models.Reason;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class StorageServiceSubscription extends AbstractAzServiceSubscription<StorageServiceSubscription, StorageManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final StorageAccountModule storageModule;

    StorageServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureStorageAccount service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.storageModule = new StorageAccountModule(this);
    }

    StorageServiceSubscription(@Nonnull StorageManager remote, @Nonnull AzureStorageAccount service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(storageModule);
    }

    @Nonnull
    public StorageAccountModule storageAccounts() {
        return this.storageModule;
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.storageModule.getName());
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    @Nonnull
    @AzureOperation(name = "storage.check_name.name", params = {"name"}, type = AzureOperation.Type.REQUEST)
    public Availability checkNameAvailability(@Nonnull String name) {
        CheckNameAvailabilityResult result = Objects.requireNonNull(this.getRemote()).storageAccounts().checkNameAvailability(name);
        return new Availability(result.isAvailable(),
            Optional.ofNullable(result.reason()).map(Reason::toString).orElse(null), result.message());
    }
}

