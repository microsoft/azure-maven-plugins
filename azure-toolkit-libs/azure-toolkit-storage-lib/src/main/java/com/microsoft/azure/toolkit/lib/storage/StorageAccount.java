/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerModule;
import com.microsoft.azure.toolkit.lib.storage.model.AccessTier;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;
import com.microsoft.azure.toolkit.lib.storage.queue.QueueModule;
import com.microsoft.azure.toolkit.lib.storage.share.ShareModule;
import com.microsoft.azure.toolkit.lib.storage.table.TableModule;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public class StorageAccount extends AbstractAzResource<StorageAccount, StorageServiceSubscription, com.azure.resourcemanager.storage.models.StorageAccount>
    implements Deletable {

    private final BlobContainerModule blobContainerModule;
    private final ShareModule shareModule;
    private final QueueModule queueModule;
    private final TableModule tableModule;

    protected StorageAccount(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull StorageAccountModule module) {
        super(name, resourceGroupName, module);
        this.blobContainerModule = new BlobContainerModule(this);
        this.shareModule = new ShareModule(this);
        this.queueModule = new QueueModule(this);
        this.tableModule = new TableModule(this);
    }

    /**
     * copy constructor
     */
    public StorageAccount(@Nonnull StorageAccount origin) {
        super(origin);
        this.shareModule = origin.shareModule;
        this.blobContainerModule = origin.blobContainerModule;
        this.queueModule = origin.queueModule;
        this.tableModule = origin.tableModule;
    }

    protected StorageAccount(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount remote, @Nonnull StorageAccountModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.blobContainerModule = new BlobContainerModule(this);
        this.shareModule = new ShareModule(this);
        this.queueModule = new QueueModule(this);
        this.tableModule = new TableModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(this.blobContainerModule, this.shareModule, this.queueModule, this.tableModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount remote) {
        return remote.innerModel().provisioningState().toString();
    }

    @Nonnull
    public String getConnectionString() {
        // see https://github.com/Azure/azure-cli/blob/ac3b190d4d/src/azure-cli/azure/cli/command_modules/storage/operations/account.py#L232
        final AzureEnvironment environment = Azure.az(AzureCloud.class).get();
        return ResourceManagerUtils.getStorageConnectionString(this.name(), getKey(), environment);
    }

    @Nonnull
    public String getKey() {
        final com.azure.resourcemanager.storage.models.StorageAccount remote = this.getRemote();
        if (Objects.isNull(remote)) {
            throw new AzureToolkitRuntimeException(String.format("Storage Account(%s) doesn't exist.", this.getName()));
        }
        return Objects.requireNonNull(remote.getKeys().get(0).value());
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    public Performance getPerformance() {
        return remoteOptional().map(remote -> {
            String[] replicationArr = remote.skuType().name().toString().split("_");
            return replicationArr.length == 2 ? Performance.fromName(replicationArr[0]) : null;
        }).orElse(null);
    }

    public boolean canHaveQueues() {
        return remoteOptional().map(remote -> StringUtils.isNotBlank(remote.innerModel().primaryEndpoints().queue())).orElse(false);
    }

    public boolean canHaveTables() {
        return remoteOptional().map(remote -> StringUtils.isNotBlank(remote.innerModel().primaryEndpoints().table())).orElse(false);
    }

    public boolean canHaveBlobs() {
        return remoteOptional().map(remote -> StringUtils.isNotBlank(remote.innerModel().primaryEndpoints().blob())).orElse(false);
    }

    public boolean canHaveShares() {
        return remoteOptional().map(remote -> StringUtils.isNotBlank(remote.innerModel().primaryEndpoints().file())).orElse(false);
    }

    @Nullable
    public Redundancy getRedundancy() {
        return remoteOptional().map(remote -> Redundancy.fromName(remote.skuType().name().toString())).orElse(null);
    }

    @Nullable
    public Kind getKind() {
        return remoteOptional().map(remote -> Kind.fromName(remote.kind().toString())).orElse(null);
    }

    @Nullable
    public AccessTier getAccessTier() {
        return remoteOptional().map(remote -> AccessTier.valueOf(remote.accessTier().name())).orElse(null);
    }
}
