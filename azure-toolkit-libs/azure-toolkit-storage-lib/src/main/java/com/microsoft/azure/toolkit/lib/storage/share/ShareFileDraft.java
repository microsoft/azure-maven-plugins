/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class ShareFileDraft extends ShareFile implements StorageFile.Draft<ShareFile, ShareFileItem> {
    @Getter
    @Nullable
    private final ShareFile origin;
    @Setter
    private Boolean directory;
    @Setter
    private Path sourceFile;

    ShareFileDraft(@Nonnull String name, @Nonnull ShareFileModule module) {
        super(name, module);
        this.origin = null;
    }

    ShareFileDraft(@Nonnull ShareFile origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Nonnull
    @Override
    public ShareFileItem createResourceInAzure() {
        final ShareFileModule module = (ShareFileModule) this.getModule();
        final ShareDirectoryClient client = module.getClient();
        if (this.isDirectory()) {
            client.createSubdirectory(this.getName());
        } else {
            if (Objects.nonNull(sourceFile)) {
                client.createFile(this.getName(), FileUtils.sizeOf(sourceFile.toFile())).uploadFromFile(sourceFile.toString());
            } else {
                client.createFile(this.getName(), 0);
            }
        }
        return Objects.requireNonNull(module.loadResourceFromAzure(this.getName(), this.getParent().getResourceGroupName()));
    }

    @Nonnull
    @Override
    public ShareFileItem updateResourceInAzure(@Nonnull ShareFileItem origin) {
        final ShareFileModule module = (ShareFileModule) this.getModule();
        final String name = origin.getName();
        final ShareFileClient client = module.getClient().getFileClient(name);
        if (Objects.nonNull(this.sourceFile)) {
            client.deleteIfExists();
            client.create(FileUtils.sizeOf(sourceFile.toFile()));
            client.uploadFromFile(this.sourceFile.toString());
        }
        return Objects.requireNonNull(module.loadResourceFromAzure(this.getName(), this.getParent().getResourceGroupName()));
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
