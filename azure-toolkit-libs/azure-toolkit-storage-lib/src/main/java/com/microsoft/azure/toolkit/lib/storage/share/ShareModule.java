/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class ShareModule extends AbstractAzResourceModule<Share, StorageAccount, ShareClient> {

    public static final String NAME = "Azure.FileShare";
    private ShareServiceClient client;

    public ShareModule(@Nonnull StorageAccount parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    @Nullable
    synchronized ShareServiceClient getFileShareServiceClient() {
        if (Objects.isNull(this.client) && this.parent.exists()) {
            final String connectionString = this.parent.getConnectionString();
            this.client = new ShareServiceClientBuilder().connectionString(connectionString).buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Stream<ShareClient> loadResourcesFromAzure() {
        if (!this.parent.exists()) {
            return Stream.empty();
        }
        final ShareServiceClient client = this.getFileShareServiceClient();
        return Objects.requireNonNull(client).listShares().stream().map(s -> client.getShareClient(s.getName()));
    }

    @Nullable
    @Override
    protected ShareClient loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        if (!this.parent.exists()) {
            return null;
        }
        return this.loadResourcesFromAzure().filter(c -> c.getShareName().equals(name)).findAny().orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/storage.delete_share.share", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final ShareServiceClient client = this.getFileShareServiceClient();
        Objects.requireNonNull(client).deleteShare(id.name());
    }

    @Nonnull
    @Override
    protected ShareDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ShareDraft(name, this);
    }

    @Nonnull
    protected Share newResource(@Nonnull ShareClient r) {
        return new Share(r.getShareName(), this);
    }

    @Nonnull
    protected Share newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Share(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "File Share";
    }
}
