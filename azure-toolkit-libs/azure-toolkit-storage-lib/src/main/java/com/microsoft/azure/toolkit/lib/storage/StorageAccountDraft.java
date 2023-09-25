/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.SkuName;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.AccessTier;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import lombok.Data;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class StorageAccountDraft extends StorageAccount implements AzResource.Draft<StorageAccount, com.azure.resourcemanager.storage.models.StorageAccount> {
    @Getter
    @Nullable
    private final StorageAccount origin;
    @Nullable
    private Config config;

    StorageAccountDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull StorageAccountModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    StorageAccountDraft(@Nonnull StorageAccount origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/storage.create_account.account", params = {"this.getName()"})
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
        final Action<AzResource> connect = Optional.ofNullable(AzureActionManager.getInstance().getAction(AzResource.CONNECT_RESOURCE))
            .map(action -> action.bind(this)).orElse(null);
        final Action<Object> createContainer = Optional.ofNullable(AzureActionManager.getInstance().getAction(AzResource.CREATE_RESOURCE))
            .map(action -> action.bind(this.blobContainerModule).withLabel("Create Blob Container")).orElse(null);
        final Action<Object> createShare = Optional.ofNullable(AzureActionManager.getInstance().getAction(AzResource.CREATE_RESOURCE))
            .map(action -> action.bind(this.shareModule).withLabel("Create File Share")).orElse(null);
        final Object[] actions = Stream.of(connect, createContainer, createShare).filter(Objects::nonNull).toArray();
        messager.success(AzureString.format("Storage Account({0}) is successfully created.", name), actions);
        return account;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/storage.update_account.account", params = {"this.getName()"})
    public com.azure.resourcemanager.storage.models.StorageAccount updateResourceInAzure(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    public void setConfig(@Nonnull StorageAccountConfig storageAccount) {
        this.setRegion(storageAccount.getRegion());
        this.setPerformance(storageAccount.getPerformance());
        this.setKind(storageAccount.getKind());
        this.setRedundancy(storageAccount.getRedundancy());
        this.setAccessTier(storageAccount.getAccessTier());
    }

    public void setRegion(@Nonnull Region region) {
        this.ensureConfig().setRegion(region);
    }

    @Nullable
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

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getRegion()) || Objects.equals(this.config.getRegion(), super.getRegion()) ||
            Objects.isNull(this.config.getPerformance()) || Objects.equals(this.config.getPerformance(), super.getPerformance()) ||
            Objects.isNull(this.config.getKind()) || Objects.equals(this.config.getKind(), super.getKind()) ||
            Objects.isNull(this.config.getRedundancy()) || Objects.equals(this.config.getRedundancy(), super.getRedundancy()) ||
            Objects.isNull(this.config.getAccessTier()) || this.config.getAccessTier() == super.getAccessTier();
        return !notModified;
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    @Nullable
    private static class Config {
        private Region region;
        private Performance performance;
        private Kind kind;
        private Redundancy redundancy;
        private AccessTier accessTier;
    }
}