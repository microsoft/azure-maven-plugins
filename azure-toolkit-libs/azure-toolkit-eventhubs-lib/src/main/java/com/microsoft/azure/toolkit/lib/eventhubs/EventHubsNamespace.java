package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.resourcemanager.eventhubs.models.EventHubNamespace;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class EventHubsNamespace extends AbstractAzResource<EventHubsNamespace, EventHubsNamespaceSubscription, EventHubNamespace> {
    protected EventHubsNamespace(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull EventHubsNamespaceModule module) {
        super(name, resourceGroupName, module);
    }
    
    protected EventHubsNamespace(@Nonnull EventHubsNamespace namespace) {
        super(namespace);
    }

    protected EventHubsNamespace(@Nonnull EventHubNamespace remote, @Nonnull EventHubsNamespaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull EventHubNamespace remote) {
        return remote.provisioningState();
    }
}
