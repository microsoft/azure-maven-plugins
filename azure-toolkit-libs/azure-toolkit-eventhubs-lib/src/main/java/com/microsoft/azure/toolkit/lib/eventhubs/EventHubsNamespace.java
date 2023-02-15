package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.resourcemanager.eventhubs.EventHubsManager;
import com.azure.resourcemanager.eventhubs.models.EventHubNamespace;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

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

    @Nullable
    public String getConnectionString() {
        final List<String> connectionStrings = Optional.ofNullable(getRemote()).map(eventHubNamespace -> eventHubNamespace.listAuthorizationRules().stream()
                .map(eventHubNamespaceAuthorizationRule -> eventHubNamespaceAuthorizationRule.getKeys().primaryConnectionString())
                .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        final EventHubsManager manager = getParent().getRemote();
        if (connectionStrings.size() > 0) {
            return connectionStrings.get(0);
        }
        if (Objects.isNull(manager)) {
            return null;
        }
        return manager.namespaceAuthorizationRules().define(String.format("policy-%s", Utils.getTimestamp()))
                .withExistingNamespace(getResourceGroupName(), getName())
                .withSendAndListenAccess()
                .create().getKeys().primaryConnectionString();
    }
}
