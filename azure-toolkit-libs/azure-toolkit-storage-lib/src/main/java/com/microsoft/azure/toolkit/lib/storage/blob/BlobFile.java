/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class BlobFile implements Deletable, StorageFile {
    private final BlobFile parent;
    private final BlobItem remote;
    private final BlobContainer container;
    private final String name;

    public BlobFile(BlobItem remote, BlobFile parent, BlobContainer container) {
        this.remote = remote;
        this.parent = parent;
        this.container = container;
        this.name = Paths.get(remote.getName()).getFileName().toString();
    }

    @Override
    public String getPath() {
        return this.remote.getName();
    }

    @Override
    @SneakyThrows
    public String getId() {
        return String.format("%s/files/%s", this.container.getId(), URLEncoder.encode(this.getPath(), StandardCharsets.UTF_8.name()));
    }

    @Override
    public long getSize() {
        return this.remote.getProperties().getContentLength();
    }

    public boolean isDirectory() {
        return BooleanUtils.isTrue(this.remote.isPrefix());
    }

    public void deleteFromAzure() {
        getClient().deleteIfExists();
    }

    @Override
    public void download(OutputStream output) {
        getClient().downloadStream(output);
    }

    @NotNull
    private BlobClient getClient() {
        return this.container.getClient().getBlobClient(this.remote.getName());
    }

    public void delete() {
        this.deleteFromAzure();
    }

    public List<BlobFile> listFiles() {
        return this.container.getClient().listBlobsByHierarchy(this.remote.getName()).stream().map(b -> new BlobFile(b, this, this.container)).collect(Collectors.toList());
    }
}
