/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobContainerProperties;
import com.azure.storage.blob.specialized.BlobClientBase;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        return Collections.singletonList(this.subFileModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull BlobContainerClient remote) {
        return "";
    }

    @Override
    @Nullable
    public BlobContainerClient getClient() {
        final BlobContainerModule module = (BlobContainerModule) this.getModule();
        return Optional.ofNullable(module.getBlobServiceClient()).map(c -> c.getBlobContainerClient(this.getName())).orElse(null);
    }

    public boolean exists(String blobPath) {
        return Optional.ofNullable(this.getClient()).map(c -> c.getBlobClient(blobPath)).map(BlobClientBase::exists).orElse(false);
    }

    @Nullable
    @Override
    public OffsetDateTime getLastModified() {
        return this.remoteOptional().map(BlobContainerClient::getProperties).map(BlobContainerProperties::getLastModified).orElse(null);
    }

    @Override
    public String getPath() {
        return "";
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
    public String getUrl() {
        return Optional.ofNullable(this.getClient()).map(BlobContainerClient::getBlobContainerUrl).orElse("");
    }

    @Override
    public void download(OutputStream output) {
    }

    @Override
    public void download(Path dest) {
    }
}
