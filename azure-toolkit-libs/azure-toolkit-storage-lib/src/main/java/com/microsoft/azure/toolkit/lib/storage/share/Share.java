/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Share extends AbstractAzResource<Share, StorageAccount, ShareClient>
    implements Deletable {

    protected Share(@Nonnull String name, @Nonnull ShareModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    public Share(@Nonnull Share origin) {
        super(origin);
    }

    protected Share(@Nonnull ShareClient remote, @Nonnull ShareModule module) {
        super(remote.getShareName(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ShareClient remote) {
        return "OK";
    }

    public List<ShareFile> listFiles() {
        final ShareModule module = (ShareModule) this.getModule();
        final ShareServiceClient fileShareServiceClient = module.getFileShareServiceClient();
        final ShareClient shareClient = fileShareServiceClient.getShareClient(this.getName());
        final ShareDirectoryClient client = shareClient.getRootDirectoryClient();
        return client.listFilesAndDirectories().stream().map(f -> new ShareFile(f, null, this)).collect(Collectors.toList());
    }

    public ShareDirectoryClient getClient() {
        final ShareModule module = (ShareModule) this.getModule();
        final ShareServiceClient fileShareServiceClient = module.getFileShareServiceClient();
        final ShareClient shareClient = fileShareServiceClient.getShareClient(this.getName());
        return shareClient.getRootDirectoryClient();
    }
}
