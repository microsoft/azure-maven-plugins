/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.queue;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
    @AzureOperation(name = "azure/storage.create_queue.queue", params = {"this.getName()"})
    public QueueClient createResourceInAzure() {
        final QueueModule module = (QueueModule) this.getModule();
        final QueueServiceClient client = module.getQueueServiceClient();
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Queue ({0}).", this.getName()));
        final QueueClient queue = client.createQueue(this.getName());
        messager.success(AzureString.format("Queue ({0}) is successfully created.", this.getName()));
        return queue;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/storage.update_queue.queue", params = {"this.getName()"})
    public QueueClient updateResourceInAzure(@Nonnull QueueClient origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
