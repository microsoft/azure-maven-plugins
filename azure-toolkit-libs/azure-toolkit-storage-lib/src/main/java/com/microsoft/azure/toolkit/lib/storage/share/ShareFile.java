/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ShareFile implements Deletable, StorageFile {
    public static final long KB = 1024;
    public static final long MB = KB * 1024;
    public static final long GB = MB * 1024;
    private final ShareFileItem remote;
    private final ShareFile parent;
    private final Share share;
    private final ShareDirectoryClient parentClient;

    public ShareFile(ShareFileItem remote, ShareFile parent, Share share) {
        this.remote = remote;
        this.parent = parent;
        this.share = share;
        this.parentClient = parent == null ? share.getClient() : parent.parentClient.getSubdirectoryClient(parent.getName());
    }

    public String getPath() {
        if (Objects.nonNull(this.parent)) {
            return String.format("%s/%s", this.parent.getPath(), this.remote.getName());
        } else {
            return String.format("/%s", this.remote.getName());
        }
    }

    @SneakyThrows
    public String getId() {
        return String.format("%s/files/%s", this.share.getId(), URLEncoder.encode(this.getPath(), StandardCharsets.UTF_8.name()));
    }

    @Override
    public String getName() {
        return this.remote.getName();
    }

    public long getSize() {
        if (!this.isDirectory()) {
            return getFileClient().getProperties().getContentLength();
        }
        return -1;
    }

    private ShareFileClient getFileClient() {
        return this.parentClient.getFileClient(this.getName());
    }

    private ShareDirectoryClient getDirectoryClient() {
        return this.parentClient.getSubdirectoryClient(this.getName());
    }

    @Override
    public void download(OutputStream output) {
        getFileClient().download(output);
    }

    @Override
    public boolean isDirectory() {
        return this.remote.isDirectory();
    }

    public void deleteFromAzure() {
        if (this.isDirectory()) {
            this.parentClient.deleteSubdirectoryIfExists(this.getName());
        } else {
            this.parentClient.deleteFileIfExists(this.getName());
        }
    }

    @Override
    public void delete() {
        this.deleteFromAzure();
    }

    @Override
    public List<ShareFile> listFiles() {
        if (this.isDirectory()) {
            final ShareDirectoryClient client = this.parentClient.getSubdirectoryClient(this.getName());
            return client.listFilesAndDirectories().stream().map(f -> new ShareFile(f, this, this.share)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
