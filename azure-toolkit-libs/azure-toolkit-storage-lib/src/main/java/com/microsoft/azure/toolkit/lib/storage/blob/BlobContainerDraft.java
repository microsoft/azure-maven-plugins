/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlobContainerDraft extends BlobContainer implements AzResource.Draft<BlobContainer, BlobContainerClient> {
    @Getter
    @Nullable
    private final BlobContainer origin;

    BlobContainerDraft(@Nonnull String name, @Nonnull BlobContainerModule module) {
        super(name, module);
        this.origin = null;
    }

    BlobContainerDraft(@Nonnull BlobContainer origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Nonnull
    @Override
    public BlobContainerClient createResourceInAzure() {
        final BlobContainerModule module = (BlobContainerModule) this.getModule();
        final BlobServiceClient client = module.getBlobServiceClient();
        return client.createBlobContainer(this.getName());
    }

    @Nonnull
    @Override
    public BlobContainerClient updateResourceInAzure(@Nonnull BlobContainerClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
