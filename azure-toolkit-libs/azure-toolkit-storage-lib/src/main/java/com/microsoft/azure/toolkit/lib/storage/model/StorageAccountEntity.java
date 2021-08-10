/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import java.util.Optional;

public class StorageAccountEntity extends AbstractAzureEntityManager.RemoteAwareResourceEntity<StorageAccount> implements IStorageAccountEntity {

    @Nonnull
    private final ResourceId resourceId;

    public StorageAccountEntity(StorageAccount server) {
        this.resourceId = ResourceId.fromString(server.id());
        this.remote = server;
    }

    public String getId() {
        return resourceId.id();
    }

    @Override
    public String getName() {
        return resourceId.name();
    }

    public String getResourceGroupName() {
        return resourceId.resourceGroupName();
    }

    @Override
    public String getSubscriptionId() {
        return resourceId.subscriptionId();
    }

    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Override
    public Performance getPerformance() {
        return remoteOptional().map(remote -> {
            String[] replicationArr = remote.skuType().name().toString().split("_");
            return replicationArr.length == 2 ? Performance.fromName(replicationArr[0]) : null;
        }).orElse(null);
    }

    @Override
    public Redundancy getRedundancy() {
        return remoteOptional().map(remote -> Redundancy.fromName(remote.skuType().name().toString())).orElse(null);
    }

    @Override
    public Kind getKind() {
        return remoteOptional().map(remote -> Kind.fromName(remote.kind().toString())).orElse(null);
    }

    private Optional<StorageAccount> remoteOptional() {
        return Optional.ofNullable(this.remote);
    }

}
