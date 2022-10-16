/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.queue;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QueueDraft extends Queue implements AzResource.Draft<Queue, QueueClient> {
    @Getter
    @Nullable
    private final Queue origin;

    QueueDraft(@Nonnull String name, @Nonnull QueueModule module) {
        super(name, module);
        this.origin = null;
    }

    QueueDraft(@Nonnull Queue origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Nonnull
    @Override
    public QueueClient createResourceInAzure() {
        final QueueModule module = (QueueModule) this.getModule();
        final QueueServiceClient client = module.getQueueServiceClient();
        return client.createQueue(this.getName());
    }

    @Nonnull
    @Override
    public QueueClient updateResourceInAzure(@Nonnull QueueClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
