package com.microsoft.azure.toolkit.lib.servicelinker;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.servicelinker.models.LinkerResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ServiceLinker extends AbstractAzResource<ServiceLinker, ServiceLinkerSubscription, LinkerResource> {
    protected ServiceLinker(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<ServiceLinker, ServiceLinkerSubscription, LinkerResource> module) {
        super(name, resourceGroupName, module);
    }

    protected ServiceLinker(@Nonnull ServiceLinker origin) {
        super(origin);
    }

    protected ServiceLinker(@Nonnull LinkerResource remote, @Nonnull ServiceLinkerModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull LinkerResource remote) {
        return remote.provisioningState();
    }
}
