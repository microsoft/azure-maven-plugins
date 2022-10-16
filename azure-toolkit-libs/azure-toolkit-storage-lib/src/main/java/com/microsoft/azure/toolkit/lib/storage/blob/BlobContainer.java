/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlobContainer extends AbstractAzResource<BlobContainer, StorageAccount, BlobContainerClient>
    implements Deletable {

    protected BlobContainer(@Nonnull String name, @Nonnull BlobContainerModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    public BlobContainer(@Nonnull BlobContainer origin) {
        super(origin);
    }

    protected BlobContainer(@Nonnull BlobContainerClient remote, @Nonnull BlobContainerModule module) {
        super(remote.getBlobContainerName(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull BlobContainerClient remote) {
        return "OK";
    }

    public List<BlobFile> listBlobs() {
        final BlobContainerClient client = this.getClient();
        return client.listBlobsByHierarchy(null).stream().map(i -> new BlobFile(i, null, this)).collect(Collectors.toList());
    }

    public BlobContainerClient getClient() {
        final BlobContainerModule module = (BlobContainerModule) this.getModule();
        final BlobServiceClient blobServiceClient = module.getBlobServiceClient();
        return blobServiceClient.getBlobContainerClient(this.getName());
    }
}
