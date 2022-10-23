/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

public class BlobFileModule extends AbstractAzResourceModule<BlobFile, IBlobFile, BlobItem> {

    public static final String NAME = "file";

    public BlobFileModule(@Nonnull IBlobFile parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected BlobContainerClient getClient() {
        return this.parent.getClient();
    }

    @Nonnull
    @Override
    protected Stream<BlobItem> loadResourcesFromAzure() {
        return this.getClient().listBlobsByHierarchy(this.parent.getPath()).stream();
    }

    @Nullable
    @Override
    protected BlobItem loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return this.loadResourcesFromAzure()
            .filter(r -> Objects.equals(Paths.get(r.getName()).getFileName().toString(), name))
            .findAny().orElse(null);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final BlobFile file = this.get(resourceId);
        if (file != null) {
            if (BooleanUtils.isTrue(file.isDirectory())) {
                deleteDirectory(Objects.requireNonNull(file.getRemote()));
                this.getClient().listBlobsByHierarchy(file.getPath()).stream()
                    .map(BlobItem::getName)
                    .forEach(p -> this.getClient().getBlobClient(p).deleteIfExists());
            } else {
                this.getClient().getBlobClient(file.getPath()).deleteIfExists();
            }
        }
    }

    private void deleteDirectory(BlobItem current) {
        final PagedIterable<BlobItem> files = this.getClient().listBlobsByHierarchy(current.getName());
        for (BlobItem file : files) {
            if (file.isPrefix()) {
                deleteDirectory(file);
            } else {
                this.getClient().getBlobClient(file.getName()).deleteIfExists();
            }
        }
        this.getClient().getBlobClient(current.getName()).deleteIfExists();
    }

    @Nonnull
    @Override
    protected AzResource.Draft<BlobFile, BlobItem> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new BlobFileDraft(name, this);
    }

    @Nonnull
    @Override
    protected BlobFile newResource(@Nonnull BlobItem item) {
        final String name = Paths.get(item.getName()).getFileName().toString();
        return new BlobFile(name, this);
    }

    @Nonnull
    protected BlobFile newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new BlobFile(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "File";
    }
}
