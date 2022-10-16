/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class BlobContainerModule extends AbstractAzResourceModule<BlobContainer, StorageAccount, BlobContainerClient> {

    public static final String NAME = "containers";
    private BlobServiceClient client;

    public BlobContainerModule(@Nonnull StorageAccount parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    synchronized BlobServiceClient getBlobServiceClient() {
        if (Objects.isNull(this.client)) {
            final String connectionString = this.parent.getConnectionString();
            this.client = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Stream<BlobContainerClient> loadResourcesFromAzure() {
        final BlobServiceClient client = this.getBlobServiceClient();
        return client.listBlobContainers().stream().map(s -> client.getBlobContainerClient(s.getName()));
    }

    @Nullable
    @Override
    protected BlobContainerClient loadResourceFromAzure(@Nonnull String name, @org.jetbrains.annotations.Nullable String resourceGroup) {
        final BlobServiceClient client = this.getBlobServiceClient();
        return client.getBlobContainerClient(name);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final BlobServiceClient client = this.getBlobServiceClient();
        client.deleteBlobContainer(id.name());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected BlobContainerDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new BlobContainerDraft(name, this);
    }

    @Nonnull
    protected BlobContainer newResource(@Nonnull BlobContainerClient r) {
        return new BlobContainer(r, this);
    }

    @Nonnull
    protected BlobContainer newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new BlobContainer(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Share";
    }
}
