package com.microsoft.azure.toolkit.lib.servicebus.queue;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.servicebus.models.Queue;
import com.azure.resourcemanager.servicebus.models.Queues;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceBusQueueModule extends AbstractAzResourceModule<ServiceBusQueue, ServiceBusNamespace, Queue> {
    public static final String NAME = "queues";
    public ServiceBusQueueModule(@Nonnull ServiceBusNamespace parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    protected Queues getClient() {
        return Optional.ofNullable(this.parent.getRemote())
                .map(com.azure.resourcemanager.servicebus.models.ServiceBusNamespace::queues).orElse(null);
    }

    @Nullable
    @Override
    protected Queue loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final List<Queue> queueList = Optional.ofNullable(this.getClient())
                .map(queues -> queues.list().stream().collect(Collectors.toList())).orElse(Collections.emptyList());
        return queueList.stream().filter(queue -> name.equals(queue.name())).findAny().orElse(null);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Queue>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator())
                .orElse(Collections.emptyIterator());
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.parent.getRemote()).map(com.azure.resourcemanager.servicebus.models.ServiceBusNamespace::queues)
                .ifPresent(client -> client.deleteByName(this.getName()));
    }

    @Nonnull
    @Override
    protected ServiceBusQueue newResource(@Nonnull Queue remote) {
        return new ServiceBusQueue(remote, this);
    }

    @Nonnull
    @Override
    protected ServiceBusQueue newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ServiceBusQueue(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Service Bus Queue";
    }
}
