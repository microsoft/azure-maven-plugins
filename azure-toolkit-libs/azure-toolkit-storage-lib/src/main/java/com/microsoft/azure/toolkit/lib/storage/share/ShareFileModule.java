/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;

public class ShareFileModule extends AbstractAzResourceModule<ShareFile, IShareFile, ShareFileItem> {

    public static final String NAME = "file";

    public ShareFileModule(@Nonnull IShareFile parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected ShareDirectoryClient getClient() {
        return (ShareDirectoryClient) this.parent.getClient();
    }

    @Nonnull
    @Override
    protected Stream<ShareFileItem> loadResourcesFromAzure() {
        return this.getClient().listFilesAndDirectories().stream();
    }

    @Nullable
    @Override
    protected ShareFileItem loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return this.loadResourcesFromAzure().filter(r -> r.getName().equals(name)).findAny().orElse(null);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ShareFile shareFile = this.get(resourceId);
        if (shareFile != null) {
            if (shareFile.isDirectory()) {
                deleteDirectory((ShareDirectoryClient) shareFile.getClient());
            } else {
                this.getClient().deleteFileIfExists(shareFile.getName());
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
