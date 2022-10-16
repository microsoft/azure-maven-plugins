/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
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
    public ShareClient createResourceInAzure() {
        final ShareModule module = (ShareModule) this.getModule();
        final ShareServiceClient client = module.getFileShareServiceClient();
        return client.createShare(this.getName());
    }

    @Nonnull
    @Override
    public ShareClient updateResourceInAzure(@Nonnull ShareClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
