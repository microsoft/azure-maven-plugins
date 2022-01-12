/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.SkuName;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.storage.model.AccessTier;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class StorageAccountDraft extends StorageAccount implements AzResource.Draft<StorageAccount, com.azure.resourcemanager.storage.models.StorageAccount> {
    @Nullable
    private Config config;

    StorageAccountDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull StorageAccountModule module) {
        super(name, resourceGroup, module);
        this.setStatus(IAzureBaseResource.Status.DRAFT);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Override
    public com.azure.resourcemanager.storage.models.StorageAccount createResourceInAzure() {
        final String name = this.getName();
        final StorageManager manager = Objects.requireNonNull(this.getParent().getRemote());
        com.azure.resourcemanager.storage.models.StorageAccount.DefinitionStages.WithCreate withCreate =
            manager.storageAccounts().define(name)
                .withRegion(this.getRegion().getName())
                .withExistingResourceGroup(this.getResourceGroupName())
                .withSku(StorageAccountSkuType.fromSkuName(SkuName.fromString(this.getRedundancy().getName())));
        final Kind kind = this.getKind();
        if (Objects.equals(Kind.STORAGE, kind)) {
            withCreate = withCreate.withGeneralPurposeAccountKind();
        } else if (Objects.equals(Kind.FILE_STORAGE, kind)) {
            withCreate = withCreate.withFileStorageAccountKind();
        } else if (Objects.equals(Kind.BLOCK_BLOB_STORAGE, kind)) {
            withCreate = withCreate.withBlockBlobStorageAccountKind();
        } else if (Objects.equals(Kind.BLOB_STORAGE, kind)) {
            withCreate = withCreate.withBlobStorageAccountKind().withAccessTier(
                Optional.ofNullable(this.getAccessTier()).map(t -> com.azure.resourcemanager.storage.models.AccessTier.fromString(t.toString())).orElse(null));
        } else {
            withCreate = withCreate.withGeneralPurposeAccountKindV2();
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Storage Account({0})...", name));
        final com.azure.resourcemanager.storage.models.StorageAccount account = withCreate.create();
        messager.success(AzureString.format("Storage Account({0}) is successfully created.", name));
        return account;
    }

    @Override
    public com.azure.resourcemanager.storage.models.StorageAccount updateResourceInAzure(@NotNull com.azure.resourcemanager.storage.models.StorageAccount origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    public void setConfig(StorageAccountConfig storageAccount) {
        this.setRegion(storageAccount.getRegion());
        this.setPerformance(storageAccount.getPerformance());
        this.setKind(storageAccount.getKind());
        this.setRedundancy(storageAccount.getRedundancy());
        this.setAccessTier(storageAccount.getAccessTier());
    }

    public void setRegion(@Nonnull Region region) {
        this.ensureConfig().setRegion(region);
    }

    @Nonnull
    public Region getRegion() {
        return Objects.requireNonNull(Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion));
    }

    @Nullable
    @Override
    public Performance getPerformance() {
        return Optional.ofNullable(config).map(Config::getPerformance).orElseGet(super::getPerformance);
    }

    public void setPerformance(@Nonnull Performance performance) {
        this.ensureConfig().setPerformance(performance);
    }

    @Nullable
    @Override
    public Kind getKind() {
        return Optional.ofNullable(config).map(Config::getKind).orElseGet(super::getKind);
    }

    public void setKind(@Nonnull Kind kind) {
        this.ensureConfig().setKind(kind);
    }

    @Nullable
    @Override
    public Redundancy getRedundancy() {
        return Optional.ofNullable(config).map(Config::getRedundancy).orElseGet(super::getRedundancy);
    }

    public void setRedundancy(@Nonnull Redundancy redundancy) {
        this.ensureConfig().setRedundancy(redundancy);
    }

    @Nullable
    @Override
    public AccessTier getAccessTier() {
        return Optional.ofNullable(config).map(Config::getAccessTier).orElseGet(super::getAccessTier);
    }

    public void setAccessTier(@Nonnull AccessTier tier) {
        this.ensureConfig().setAccessTier(tier);
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config {
        private Region region;
        private Performance performance;
        private Kind kind;
        private Redundancy redundancy;
        private AccessTier accessTier;
    }
}