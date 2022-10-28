/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ShareDraft extends Share implements AzResource.Draft<Share, ShareClient> {
    @Getter
    @Nullable
    private final Share origin;

    ShareDraft(@Nonnull String name, @Nonnull ShareModule module) {
        super(name, module);
        this.origin = null;
    }

    ShareDraft(@Nonnull Share origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Nonnull
    @Override
    @AzureOperation(name = "storage.create_share.share", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public ShareClient createResourceInAzure() {
        final ShareModule module = (ShareModule) this.getModule();
        final ShareServiceClient client = module.getFileShareServiceClient();
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating File Share ({0}).", this.getName()));
        final ShareClient share = client.createShare(this.getName());
        messager.success(AzureString.format("File Share ({0}) is successfully created.", this.getName()));
        return share;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "storage.update_share.share", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public ShareClient updateResourceInAzure(@Nonnull ShareClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
