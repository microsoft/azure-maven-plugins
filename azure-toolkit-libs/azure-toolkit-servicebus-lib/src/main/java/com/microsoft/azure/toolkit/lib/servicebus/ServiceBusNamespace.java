package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ServiceBusNamespace extends AbstractAzResource<ServiceBusNamespace, ServiceBusNamespaceSubscription, com.azure.resourcemanager.servicebus.models.ServiceBusNamespace> implements Deletable {
    protected ServiceBusNamespace(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ServiceBusNamespaceModule module) {
        super(name, resourceGroupName, module);
    }

    protected ServiceBusNamespace(@Nonnull ServiceBusNamespace origin) {
        super(origin);
    }

    protected ServiceBusNamespace(@Nonnull com.azure.resourcemanager.servicebus.models.ServiceBusNamespace remote, @Nonnull ServiceBusNamespaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull com.azure.resourcemanager.servicebus.models.ServiceBusNamespace remote) {
        return remote.innerModel().status();
    }
}
