package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.resourcemanager.eventhubs.models.EventHub;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class EventHubsInstance extends AbstractAzResource<EventHubsInstance, EventHubsNamespace, EventHub> {
    protected EventHubsInstance(@Nonnull String name, @Nonnull EventHubsInstanceModule module) {
        super(name, module);
    }

    protected EventHubsInstance(@Nonnull EventHub remote, @Nonnull EventHubsInstanceModule module) {
        super(remote.name(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull EventHub remote) {
        return remote.innerModel().status().toString();
    }
}
