/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Getter
public class BlobFile extends AbstractAzResource<BlobFile, IBlobFile, BlobItem> implements Deletable, IBlobFile {
    private final BlobFileModule subFileModule;

    protected BlobFile(@Nonnull String name, @Nonnull BlobFileModule module) {
        super(name, module);
        this.subFileModule = new BlobFileModule(this);
    }

    /**
     * copy constructor
     */
    public BlobFile(@Nonnull BlobFile origin) {
        super(origin);
        this.subFileModule = origin.subFileModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return this.isDirectory() ? Collections.singletonList(this.subFileModule) : Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull BlobItem remote) {
        return "OK";
    }

    public long getSize() {
        if (!this.isDirectory()) {
            return this.remoteOptional().map(r -> r.getProperties().getContentLength()).orElse(-1L);
        }
        return -1;
    }

    @Override
    public void download(OutputStream output) {
        this.getClient().getBlobClient(this.getPath()).downloadStream(output);
    }

    @Override
    public void download(Path dest) {
        this.getClient().getBlobClient(this.getPath()).downloadToFile(dest.toAbsolutePath().toString());
    }

    @Nonnull
    public BlobContainerClient getClient() {
        return this.getParent().getClient();
    }

    @Override
    public String getPath() {
        return this.remoteOptional().map(BlobItem::getName).orElse(Paths.get(this.getParent().getPath(), this.getName()).toString());
    }

    @Override
    public String getUrl() {
        return this.getClient().getBlobClient(this.getPath()).getBlobUrl();
    }

    @Override
    public BlobContainer getContainer() {
        return this.getParent().getContainer();
    }

    public boolean isDirectory() {
        return this.remoteOptional().map(r -> BooleanUtils.isTrue(r.isPrefix())).orElse(false);
    }
}
