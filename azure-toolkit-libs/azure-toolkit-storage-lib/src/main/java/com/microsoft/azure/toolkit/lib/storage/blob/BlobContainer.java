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
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

@Getter
public class BlobContainer extends AbstractAzResource<BlobContainer, StorageAccount, BlobContainerClient>
    implements Deletable, IBlobFile {

    private final BlobFileModule subFileModule;

    protected BlobContainer(@Nonnull String name, @Nonnull BlobContainerModule module) {
        super(name, module);
        this.subFileModule = new BlobFileModule(this);
    }

    /**
     * copy constructor
     */
    public BlobContainer(@Nonnull BlobContainer origin) {
        super(origin);
        this.subFileModule = origin.subFileModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull BlobContainerClient remote) {
        return "";
    }

    public BlobContainerClient getClient() {
        final BlobContainerModule module = (BlobContainerModule) this.getModule();
        final BlobServiceClient blobServiceClient = module.getBlobServiceClient();
        return blobServiceClient.getBlobContainerClient(this.getName());
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public BlobContainer getContainer() {
        return this;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public void download(OutputStream output) {
    }
}
