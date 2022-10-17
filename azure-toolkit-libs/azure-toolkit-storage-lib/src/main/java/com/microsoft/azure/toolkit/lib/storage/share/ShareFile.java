/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

@Getter
public class ShareFile extends AbstractAzResource<ShareFile, IShareFile, ShareFileItem> implements Deletable, IShareFile {
    private final ShareFileModule subFileModule;

    protected ShareFile(@Nonnull String name, @Nonnull ShareFileModule module) {
        super(name, module);
        this.subFileModule = new ShareFileModule(this);
    }

    /**
     * copy constructor
     */
    public ShareFile(@Nonnull ShareFile origin) {
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
    public String loadStatus(@Nonnull ShareFileItem remote) {
        return "OK";
    }

    public long getSize() {
        if (!this.isDirectory()) {
            return this.remoteOptional().map(ShareFileItem::getFileSize).orElse(-1L);
        }
        return -1;
    }

    @Override
    public void download(OutputStream output) {
        if (!this.isDirectory()) {
            final ShareDirectoryClient parentClient = (ShareDirectoryClient) this.getParent().getClient();
            parentClient.getFileClient(this.getName()).download(output);
        }
    }

    @Override
    public Object getClient() {
        final ShareDirectoryClient parentClient = (ShareDirectoryClient) this.getParent().getClient();
        return this.isDirectory() ? parentClient.getSubdirectoryClient(this.getName()) : parentClient.getFileClient(this.getName());
    }

    @Override
    public Share getShare() {
        return this.getParent().getShare();
    }

    @Override
    public boolean isDirectory() {
        return this.remoteOptional().map(ShareFileItem::isDirectory).orElse(false);
    }
}
