/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.queue;

import com.azure.core.util.Context;
import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.azure.storage.queue.models.QueuesSegmentOptions;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public class QueueModule extends AbstractAzResourceModule<Queue, StorageAccount, QueueClient> {

    public static final String NAME = "Azure.Queue";
    private QueueServiceClient client;

    public QueueModule(@Nonnull StorageAccount parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    @Nullable
    synchronized QueueServiceClient getQueueServiceClient() {
        if (Objects.isNull(this.client) && this.parent.exists()) {
            final String connectionString = this.parent.getConnectionString();
            this.client = new QueueServiceClientBuilder().connectionString(connectionString).buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, QueueClient>> loadResourcePagesFromAzure() {
        if (!this.parent.exists()) {
            return Collections.emptyIterator();
        }
        final QueueServiceClient client = this.getQueueServiceClient();
        return Objects.requireNonNull(client).listQueues(new QueuesSegmentOptions().setIncludeMetadata(true),null, Context.NONE).streamByPage(getPageSize())
            .map(p -> new ItemPage<>(p.getValue().stream().map(s -> client.getQueueClient(s.getName()))))
            .iterator();
    }

    @Nullable
    @Override
    protected QueueClient loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        if (!this.parent.exists()) {
            return null;
        }
        final QueueServiceClient client = this.getQueueServiceClient();
        Stream<QueueClient> resources = Objects.requireNonNull(client).listQueues(new QueuesSegmentOptions().setIncludeMetadata(true),null, Context.NONE).stream().map(s -> client.getQueueClient(s.getName()));
        return resources.filter(c -> c.getQueueName().equals(name)).findAny().orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/storage.delete_queue.queue", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final QueueServiceClient client = this.getQueueServiceClient();
        Objects.requireNonNull(client).deleteQueue(id.name());
    }

    @Nonnull
    @Override
    protected QueueDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new QueueDraft(name, this);
    }

    @Nonnull
    protected Queue newResource(@Nonnull QueueClient r) {
        return new Queue(r, this);
    }

    @Nonnull
    protected Queue newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Queue(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Queue";
    }
}
