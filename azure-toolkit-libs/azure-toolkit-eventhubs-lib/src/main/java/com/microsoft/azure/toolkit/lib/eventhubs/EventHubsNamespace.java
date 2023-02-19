package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.resourcemanager.eventhubs.models.EventHubNamespace;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class EventHubsNamespace extends AbstractAzResource<EventHubsNamespace, EventHubsNamespaceSubscription, EventHubNamespace> implements Deletable {
    @Nonnull
    private final EventHubsInstanceModule instanceModule;
    protected EventHubsNamespace(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull EventHubsNamespaceModule module) {
        super(name, resourceGroupName, module);
        this.instanceModule = new EventHubsInstanceModule(this);
    }
    
    protected EventHubsNamespace(@Nonnull EventHubsNamespace origin) {
        super(origin);
        this.instanceModule = origin.instanceModule;
    }

    protected EventHubsNamespace(@Nonnull EventHubNamespace remote, @Nonnull EventHubsNamespaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.instanceModule = new EventHubsInstanceModule(this);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull EventHubNamespace remote) {
        return remote.innerModel().status();
    }

    public List<EventHubsInstance> getInstances() {
        return this.instanceModule.list();
    }
}
