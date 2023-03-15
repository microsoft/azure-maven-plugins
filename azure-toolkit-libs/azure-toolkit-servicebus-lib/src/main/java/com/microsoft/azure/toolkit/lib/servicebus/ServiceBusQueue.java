package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.resourcemanager.servicebus.models.Queue;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ServiceBusQueue extends AbstractAzResource<ServiceBusQueue, ServiceBusNamespace, Queue> {
    protected ServiceBusQueue(@Nonnull String name, @Nonnull ServiceBusQueueModule module) {
        super(name, module);
    }

    protected ServiceBusQueue(@Nonnull Queue remote, @Nonnull ServiceBusQueueModule module) {
        super(remote.name(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Queue remote) {
        return remote.innerModel().status().toString();
    }
}
