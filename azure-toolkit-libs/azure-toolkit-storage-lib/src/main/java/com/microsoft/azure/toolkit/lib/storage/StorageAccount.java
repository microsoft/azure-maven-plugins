/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.AccessTier;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StorageAccount extends AbstractAzResource<StorageAccount, StorageResourceManager, com.azure.resourcemanager.storage.models.StorageAccount>
    implements Removable {

    protected StorageAccount(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull StorageAccountModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    public StorageAccount(@Nonnull StorageAccount origin) {
        super(origin);
    }

    protected StorageAccount(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount remote, @Nonnull StorageAccountModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.setRemote(remote);
    }

    @Override
    public List<AzResourceModule<?, StorageAccount, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount remote) {
        return remote.innerModel().provisioningState().toString();
    }

    @Override
    public String status() {
        return this.getStatus();
    }

    @AzureOperation(name = "storage.get_connection_string.account", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public String getConnectionString() {
        // see https://github.com/Azure/azure-cli/blob/ac3b190d4d/src/azure-cli/azure/cli/command_modules/storage/operations/account.py#L232
        final AzureEnvironment environment = Azure.az(AzureCloud.class).get();
        return ResourceManagerUtils.getStorageConnectionString(this.name(), getKey(), environment);
    }

    @AzureOperation(name = "storage.get_key.account", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public String getKey() {
        return Objects.requireNonNull(this.getRemote()).getKeys().get(0).value();
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

    @Override
    public void remove() {
        this.delete();
    }
}
