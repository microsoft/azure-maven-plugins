/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.compute.models.ComputeResourceType;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.KnownWindowsVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachines;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.AbstractAzureResourceModule;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AzureVirtualMachine extends AbstractAzureResourceModule<VirtualMachine> implements AzureOperationEvent.Source<AzureVirtualMachine> {

    private static final List<AzureImage> linuxImages =
            Arrays.stream(KnownLinuxVirtualMachineImage.values()).map(AzureImage::new).collect(Collectors.toList());
    private static final List<AzureImage> windowsImages =
            Arrays.stream(KnownWindowsVirtualMachineImage.values()).map(AzureImage::new).collect(Collectors.toList());
    private static final List<AzureImage> images =
            Collections.unmodifiableList(Stream.of(linuxImages, windowsImages).flatMap(List::stream).collect(Collectors.toList()));

    public AzureVirtualMachine() { // for SPI
        super(AzureVirtualMachine::new);
    }

    private AzureVirtualMachine(@Nonnull final List<Subscription> subscriptions) {
        super(AzureVirtualMachine::new, subscriptions);
    }

    @Cacheable(cacheName = "compute/{}/vm", key = "$subscriptionId")
    public List<VirtualMachine> list(@Nonnull final String subscriptionId, boolean... force) {
        final VirtualMachines virtualMachines = getVirtualMachinesManager(subscriptionId);
        return virtualMachines.list().stream()
                .map(vm -> new VirtualMachine(vm, this)).collect(Collectors.toList());
    }

    @Nonnull
    public VirtualMachine get(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        final VirtualMachines virtualMachinesManager = getVirtualMachinesManager(subscriptionId);
        return new VirtualMachine(virtualMachinesManager.getByResourceGroup(resourceGroup, name), this);
    }

    public List<String> availabilitySets() {
        return availabilitySets(getDefaultSubscription().getId());
    }

    public List<String> availabilitySets(@Nonnull final String subscriptionId) {
        return getVirtualMachinesManager(subscriptionId).manager().availabilitySets().list().stream().map(AvailabilitySet::name).collect(Collectors.toList());
    }

    public List<AzureImagePublisher> publishers(final Region region) {
        return publishers(getDefaultSubscription().getId(), region);
    }

    public List<AzureImagePublisher> publishers(final String subscriptionId, final Region region) {
        return getVirtualMachinesManager(subscriptionId).manager().virtualMachineImages()
                .publishers().listByRegion(region.getName()).stream().map(AzureImagePublisher::new).collect(Collectors.toList());
    }

    public List<AzureVirtualMachineSize> listPricing(final Region region) {
        return listPricing(getDefaultSubscription().getId(), region);
    }

    public List<AzureVirtualMachineSize> listPricing(final String subscriptionId, final Region region) {
        return getVirtualMachinesManager(subscriptionId).manager().computeSkus()
                .listByRegionAndResourceType(com.azure.core.management.Region.fromName(region.getName()), ComputeResourceType.VIRTUALMACHINES).stream()
                .map(AzureVirtualMachineSize::new).collect(Collectors.toList());
    }

    public List<AzureImage> getKnownImages() {
        return images;
    }

    @Nonnull
    public VirtualMachine create(@Nonnull final DraftVirtualMachine config) {
        return config.create(this);
    }

    VirtualMachines getVirtualMachinesManager(String subscriptionId) {
        return getResourceManager(subscriptionId, ComputeManager::configure, ComputeManager.Configurable::authenticate).virtualMachines();
    }

    @AzureOperation(name = "service.refresh", params = "this.name()", type = AzureOperation.Type.SERVICE)
    public void refresh() {
        try {
            CacheManager.evictCache("compute/{}/vm", CacheEvict.ALL);
        } catch (ExecutionException e) {
            log.warn("failed to evict cache", e);
        }
    }

    @Override
    public String name() {
        return "Virtual Machine";
    }
}
