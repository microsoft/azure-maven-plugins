/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
    @AzureOperation(name = "storage.create_share_file_in_azure.file", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public ShareFileItem createResourceInAzure() {
        final ShareFileModule module = (ShareFileModule) this.getModule();
        final ShareDirectoryClient client = module.getClient();
        if (Objects.isNull(client)) {
            throw new AzureToolkitRuntimeException(String.format("parent directory(%s) doesn't exist.", module.getParent().getName()));
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        if (this.isDirectory()) {
            messager.info(AzureString.format("Start creating directory ({0}).", this.getName()));
            client.createSubdirectory(this.getName());
            messager.success(AzureString.format("Directory ({0}) is successfully created.", this.getName()));
        } else {
            if (Objects.nonNull(sourceFile)) {
                messager.info(AzureString.format("Start uploading file ({0}).", sourceFile.getFileName()));
                client.createFile(this.getName(), FileUtils.sizeOf(sourceFile.toFile())).uploadFromFile(sourceFile.toString());
                messager.success(AzureString.format("File ({0}) is successfully uploaded.", sourceFile.getFileName()));
            } else {
                messager.info(AzureString.format("Start creating file ({0}).", this.getName()));
                client.createFile(this.getName(), 0);
                messager.success(AzureString.format("File ({0}) is successfully created.", this.getName()));
            }
        }
        return Objects.requireNonNull(module.loadResourceFromAzure(this.getName(), this.getParent().getResourceGroupName()));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "storage.update_share_file_in_azure.file", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public ShareFileItem updateResourceInAzure(@Nonnull ShareFileItem origin) {
        final ShareFileModule module = (ShareFileModule) this.getModule();
        final String name = origin.getName();
        final ShareDirectoryClient dirClient = module.getClient();
        if (Objects.isNull(dirClient)) {
            throw new AzureToolkitRuntimeException(String.format("parent directory(%s) doesn't exist.", module.getParent().getName()));
        }
        final ShareFileClient client = dirClient.getFileClient(name);
        if (Objects.nonNull(this.sourceFile)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating file ({0})", this.getName()));
            client.deleteIfExists();
            client.create(FileUtils.sizeOf(sourceFile.toFile()));
            client.uploadFromFile(this.sourceFile.toString());
            messager.success(AzureString.format("File ({0}) is successfully updated.", this.getName()));
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
