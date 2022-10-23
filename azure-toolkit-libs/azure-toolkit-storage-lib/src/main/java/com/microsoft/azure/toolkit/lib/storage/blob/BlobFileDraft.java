/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class BlobFileDraft extends BlobFile implements StorageFile.Draft<BlobFile, BlobItem> {
    @Getter
    @Nullable
    private final BlobFile origin;
    @Setter
    private Boolean directory;
    @Setter
    private String relativePath;
    @Setter
    private Path sourceFile;

    BlobFileDraft(@Nonnull String name, @Nonnull BlobFileModule module) {
        super(name, module);
        this.origin = null;
    }

    BlobFileDraft(@Nonnull BlobFile origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Nonnull
    @Override
    public BlobItem createResourceInAzure() {
        final BlobFileModule module = (BlobFileModule) this.getModule();
        final String fullPath = Paths.get(this.getParent().getPath(), StringUtils.firstNonBlank(this.relativePath, this.getName())).toString();
        final BlobClient client = module.getClient().getBlobClient(fullPath);
        if (Objects.nonNull(this.sourceFile)) {
            client.uploadFromFile(this.sourceFile.toString());
        } else {
            client.upload(BinaryData.fromString(""));
        }
        return Objects.requireNonNull(module.loadResourceFromAzure(this.getName(), this.getParent().getResourceGroupName()));
    }

    @Nonnull
    @Override
    public BlobItem updateResourceInAzure(@Nonnull BlobItem origin) {
        final BlobFileModule module = (BlobFileModule) this.getModule();
        final String fullPath = origin.getName();
        final BlobClient client = module.getClient().getBlobClient(fullPath);
        if (Objects.nonNull(this.sourceFile)) {
            client.uploadFromFile(this.sourceFile.toString(), true);
        }
        return origin;
    }

    @Override
    public boolean isDirectory() {
        return Optional.ofNullable(this.directory).orElseGet(super::isDirectory);
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
