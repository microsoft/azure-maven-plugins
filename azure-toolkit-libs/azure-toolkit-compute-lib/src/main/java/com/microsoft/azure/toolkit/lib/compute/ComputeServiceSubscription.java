/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.compute.models.ComputeResourceType;
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineModule;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImagePublisher;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmSize;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class ComputeServiceSubscription extends AbstractAzServiceSubscription<ComputeServiceSubscription, ComputeManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final VirtualMachineModule virtualMachineModule;

    ComputeServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureCompute service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.virtualMachineModule = new VirtualMachineModule(this);
    }

    ComputeServiceSubscription(@Nonnull ComputeManager remote, @Nonnull AzureCompute service) {
        this(remote.subscriptionId(), service);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, ComputeServiceSubscription, ?>> getSubModules() {
        return Collections.singletonList(virtualMachineModule);
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    public List<String> listAvailabilitySets() {
        return Objects.requireNonNull(this.getRemote())
            .availabilitySets().list().stream().map(AvailabilitySet::name).collect(Collectors.toList());
    }

    public List<VmImagePublisher> listPublishers(final Region region) {
        return Objects.requireNonNull(this.getRemote()).virtualMachineImages()
            .publishers().listByRegion(region.getName()).stream().map(VmImagePublisher::new).collect(Collectors.toList());
    }

    public List<VmSize> listSizes(final Region region) {
        return Objects.requireNonNull(this.getRemote())
            .computeSkus()
            .listByRegionAndResourceType(com.azure.core.management.Region.fromName(region.getName()), ComputeResourceType.VIRTUALMACHINES).stream()
            .map(VmSize::new).collect(Collectors.toList());
    }
}

