/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.KnownWindowsVirtualMachineImage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineModule;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImage;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImagePublisher;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmSize;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AzureCompute extends AbstractAzService<ComputeResourceManager, ComputeManager> {
    private static final List<VmImage> linuxImages =
        Arrays.stream(KnownLinuxVirtualMachineImage.values()).map(VmImage::new).collect(Collectors.toList());
    private static final List<VmImage> windowsImages =
        Arrays.stream(KnownWindowsVirtualMachineImage.values()).map(VmImage::new).collect(Collectors.toList());
    private static final List<VmImage> images =
        Collections.unmodifiableList(Stream.of(linuxImages, windowsImages).flatMap(List::stream).collect(Collectors.toList()));

    public AzureCompute() {
        super("Microsoft.Compute");
    }

    @Nonnull
    @Override
    protected ComputeManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return ComputeManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
            .withLogLevel(logLevel)
            .withPolicy(AzureService.getUserAgentPolicy(userAgent)) // set user agent with policy
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected ComputeResourceManager newResource(@Nonnull ComputeManager remote) {
        return new ComputeResourceManager(remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Virtual machines";
    }

    @Nonnull
    public VirtualMachineModule virtualMachines(@Nonnull String subscriptionId) {
        final ComputeResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getVirtualMachineModule();
    }

    @Nonnull
    public List<VirtualMachine> virtualMachines() {
        return this.list().stream().flatMap(m -> m.getVirtualMachineModule().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    public VirtualMachine virtualMachine(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return Optional.ofNullable(this.get(id.subscriptionId(), id.resourceGroupName()))
            .map(ComputeResourceManager::getVirtualMachineModule)
            .map(m -> m.get(id.name(), id.resourceGroupName())).orElse(null);
    }

    @Cacheable(cacheName = "vm/{}/availabilitySets", key = "${subscriptionId}")
    public List<String> listAvailabilitySets(@Nonnull final String subscriptionId) {
        final ComputeResourceManager rm = get(subscriptionId, null);
        return Optional.ofNullable(rm).map(ComputeResourceManager::listAvailabilitySets).orElse(Collections.emptyList());
    }

    @Cacheable(cacheName = "vm/{}/publishers", key = "${subscriptionId}/${region.getName()}")
    public List<VmImagePublisher> listPublishers(@Nonnull final String subscriptionId, @Nonnull final Region region) {
        final ComputeResourceManager rm = get(subscriptionId, null);
        return Optional.ofNullable(rm).map(m -> m.listPublishers(region)).orElse(Collections.emptyList());
    }

    @Cacheable(cacheName = "vm/{}/sizes", key = "${subscriptionId}/${region.getName()}")
    public List<VmSize> listSizes(@Nonnull final String subscriptionId, @Nonnull final Region region) {
        final ComputeResourceManager rm = get(subscriptionId, null);
        return Optional.ofNullable(rm).map(m -> m.listSizes(region)).orElse(Collections.emptyList());
    }

    public static List<VmImage> getKnownImages() {
        return images;
    }
}
