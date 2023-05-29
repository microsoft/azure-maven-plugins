/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.specialized.BlobClientBase;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractEmulatableAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class BlobFile extends AbstractEmulatableAzResource<BlobFile, IBlobFile, BlobItem> implements Deletable, IBlobFile {
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

    @Override
    public long getSize() {
        if (!this.isDirectory()) {
            return this.remoteOptional().map(r -> r.getProperties().getContentLength()).orElse(-1L);
        }
        return -1;
    }

    @Override
    @Nullable
    public OffsetDateTime getLastModified() {
        return this.remoteOptional().map(BlobItem::getProperties).map(BlobItemProperties::getLastModified).orElse(null);
    }

    @Override
    @Nullable
    public OffsetDateTime getCreationTime() {
        return this.remoteOptional().map(BlobItem::getProperties).map(BlobItemProperties::getCreationTime).orElse(null);
    }

    @Override
    public void download(OutputStream output) {
        Optional.ofNullable(this.getClient()).map(c -> c.getBlobClient(this.getPath())).ifPresent(client -> client.downloadStream(output));
    }

    @Override
    public void download(Path dest) {
        Optional.ofNullable(this.getClient()).map(c -> c.getBlobClient(this.getPath())).ifPresent(client -> client.downloadToFile(dest.toAbsolutePath().toString()));
    }

    @Override
    @Nullable
    public BlobContainerClient getClient() {
        return this.getParent().getClient();
    }

    @Override
    public String getPath() {
        return this.remoteOptional().map(BlobItem::getName).orElse(Paths.get(this.getParent().getPath(), this.getName()).toString());
    }

    @Override
    public String getUrl() {
        return Optional.ofNullable(this.getClient()).map(c -> c.getBlobClient(this.getPath())).map(BlobClientBase::getBlobUrl).orElse("");
    }

    @Override
    public BlobContainer getContainer() {
        return this.getParent().getContainer();
    }

    public boolean isDirectory() {
        return this.remoteOptional().map(r -> BooleanUtils.isTrue(r.isPrefix())).orElse(false);
    }
}
