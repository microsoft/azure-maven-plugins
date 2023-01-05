/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.core.http.rest.Page;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ShareFileModule extends AbstractAzResourceModule<ShareFile, IShareFile, ShareFileItem> {

    public static final String NAME = "file";

    public ShareFileModule(@Nonnull IShareFile parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    protected ShareDirectoryClient getClient() {
        return (ShareDirectoryClient) this.parent.getClient();
    }

    @Nonnull
    @Override
    protected Iterator<? extends Page<ShareFileItem>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(ShareDirectoryClient::listFilesAndDirectories)
            .map(p -> p.streamByPage(PAGE_SIZE).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    protected Stream<ShareFileItem> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(ShareDirectoryClient::listFilesAndDirectories).map(PagedIterable::stream).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected ShareFileItem loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return this.loadResourcesFromAzure().filter(r -> r.getName().equals(name)).findAny().orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/storage.delete_share_file.file", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ShareFile shareFile = this.get(resourceId);
        if (shareFile != null && shareFile.exists()) {
            if (shareFile.isDirectory()) {
                deleteDirectory((ShareDirectoryClient) Objects.requireNonNull(shareFile.getClient()));
            } else {
                Objects.requireNonNull(this.getClient()).deleteFileIfExists(shareFile.getName());
            }
        }
    }

    private void deleteDirectory(ShareDirectoryClient client) {
        final PagedIterable<ShareFileItem> files = client.listFilesAndDirectories();
        for (ShareFileItem file : files) {
            if (file.isDirectory()) {
                deleteDirectory(client.getSubdirectoryClient(file.getName()));
            } else {
                client.getFileClient(file.getName()).delete();
            }
        }
        client.deleteIfExists();
    }

    @Nonnull
    @Override
    protected AzResource.Draft<ShareFile, ShareFileItem> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new ShareFileDraft(name, this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<ShareFile, ShareFileItem> newDraftForUpdate(@Nonnull ShareFile shareFile) {
        return new ShareFileDraft(shareFile);
    }

    @Nonnull
    @Override
    protected ShareFile newResource(@Nonnull ShareFileItem item) {
        return new ShareFile(item.getName(), this);
    }

    @Nonnull
    protected ShareFile newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ShareFile(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "File";
    }
}
