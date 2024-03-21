/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

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
    @AzureOperation(name = "azure/storage.create_blob_container.container", params = {"this.getName()"})
    public BlobContainerClient createResourceInAzure() {
        final BlobContainerModule module = (BlobContainerModule) this.getModule();
        final BlobServiceClient client = module.getBlobServiceClient();
        if (Objects.isNull(client)) {
            throw new AzureToolkitRuntimeException(String.format("Failed to create Blob Container (%s) because storage account (%s) doesn't exist.", this.getName(), module.getParent().getName()));
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Blob Container ({0}).", this.getName()));
        final BlobContainerClient container = client.createBlobContainer(this.getName());
        final Action<IBlobFile> create = Optional.ofNullable(AzureActionManager.getInstance().getAction(CREATE_BLOB))
            .map(action -> action.bind(this)).orElse(null);
        final Action<StorageFile> upload = Optional.ofNullable(AzureActionManager.getInstance().getAction(UPLOAD_FILES))
            .map(action -> action.bind(this)).orElse(null);
        messager.success(AzureString.format("Blob Container ({0}) is successfully created.", this.getName()), create, upload);
        return container;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/storage.update_blob_container.container", params = {"this.getName()"})
    public BlobContainerClient updateResourceInAzure(@Nonnull BlobContainerClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
