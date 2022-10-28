/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
    @AzureOperation(name = "storage.create_blob.blob", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public BlobItem createResourceInAzure() {
        final BlobFileModule module = (BlobFileModule) this.getModule();
        final String fullPath = Paths.get(this.getParent().getPath(), StringUtils.firstNonBlank(this.relativePath, this.getName())).toString();
        final BlobClient client = module.getClient().getBlobClient(fullPath);
        final IAzureMessager messager = AzureMessager.getMessager();
        if (Objects.nonNull(this.sourceFile)) {
            messager.info(AzureString.format("Start uploading file ({0}).", sourceFile.getFileName()));
            client.uploadFromFile(this.sourceFile.toString());
            messager.success(AzureString.format("File ({0}) is successfully uploaded.", sourceFile.getFileName()));
        } else {
            messager.info(AzureString.format("Start creating Blob ({0}).", fullPath));
            client.upload(BinaryData.fromString(""));
            messager.success(AzureString.format("Blob ({0}) is successfully created.", fullPath));
        }
        return Objects.requireNonNull(module.loadResourceFromAzure(this.getName(), this.getParent().getResourceGroupName()));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "storage.update_blob.blob", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public BlobItem updateResourceInAzure(@Nonnull BlobItem origin) {
        final BlobFileModule module = (BlobFileModule) this.getModule();
        final String fullPath = origin.getName();
        final BlobClient client = module.getClient().getBlobClient(fullPath);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start updating Blob ({0})", fullPath));
        if (Objects.nonNull(this.sourceFile)) {
            client.uploadFromFile(this.sourceFile.toString(), true);
        }
        messager.info(AzureString.format("Blob ({0}) is successfully updated.", fullPath));
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
