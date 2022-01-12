/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.CheckNameAvailabilityResult;
import com.azure.resourcemanager.storage.models.Reason;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.IResourceManager;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class StorageResourceManager extends AbstractAzResource<StorageResourceManager, AzResource.None, StorageManager>
    implements IResourceManager<StorageResourceManager, AzResource.None, StorageManager> {
    @Nonnull
    private final String subscriptionId;
    private final StorageAccountModule storageModule;

    StorageResourceManager(@Nonnull String subscriptionId, AzureStorageAccount service) {
        super(subscriptionId, AzResource.RESOURCE_GROUP_PLACEHOLDER, service);
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

    @Nonnull
    @Override
    public String loadStatus(@Nonnull StorageManager remote) {
        return Status.UNKNOWN;
    }

    public StorageAccountModule storageAccounts() {
        return this.storageModule;
    }

    public List<Region> listSupportedRegions() {
        return IResourceManager.super.listSupportedRegions(this.storageModule.getName());
    }

    @Override
    public AzService getService() {
        return (AzureStorageAccount) this.getModule();
    }

    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String name) {
        CheckNameAvailabilityResult result = Objects.requireNonNull(this.getRemote()).storageAccounts().checkNameAvailability(name);
        return new CheckNameAvailabilityResultEntity(result.isAvailable(),
            Optional.ofNullable(result.reason()).map(Reason::toString).orElse(null), result.message());
    }
}

