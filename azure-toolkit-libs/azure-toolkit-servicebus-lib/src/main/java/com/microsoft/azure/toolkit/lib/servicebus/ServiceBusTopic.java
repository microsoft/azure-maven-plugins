package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.resourcemanager.servicebus.models.Topic;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ServiceBusTopic extends AbstractAzResource<ServiceBusTopic, ServiceBusNamespace, Topic> {
    protected ServiceBusTopic(@Nonnull String name, @Nonnull ServiceBusTopicModule module) {
        super(name, module);
    }

    protected ServiceBusTopic(@Nonnull Topic remote, @Nonnull ServiceBusTopicModule module) {
        super(remote.name(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Topic remote) {
        return remote.innerModel().status().toString();
    }
}
