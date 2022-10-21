/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.queue;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class QueueModule extends AbstractAzResourceModule<Queue, StorageAccount, QueueClient> {

    public static final String NAME = "queues";
    private QueueServiceClient client;

    public QueueModule(@Nonnull StorageAccount parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    synchronized QueueServiceClient getQueueServiceClient() {
        if (Objects.isNull(this.client)) {
            final String connectionString = this.parent.getConnectionString();
            this.client = new QueueServiceClientBuilder().connectionString(connectionString).buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Stream<QueueClient> loadResourcesFromAzure() {
        final QueueServiceClient client = this.getQueueServiceClient();
        return client.listQueues().stream().map(s -> client.getQueueClient(s.getName()));
    }

    @Nullable
    @Override
    protected QueueClient loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return this.loadResourcesFromAzure().filter(c -> c.getQueueName().equals(name)).findAny().orElse(null);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final QueueServiceClient client = this.getQueueServiceClient();
        client.deleteQueue(id.name());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
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
