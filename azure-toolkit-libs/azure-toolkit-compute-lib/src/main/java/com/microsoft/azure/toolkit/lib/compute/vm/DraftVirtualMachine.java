/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm;

import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.compute.models.VirtualMachine.DefinitionStages.WithCreate;
import com.azure.resourcemanager.compute.models.VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged;
import com.azure.resourcemanager.compute.models.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged;
import com.azure.resourcemanager.compute.models.VirtualMachine.DefinitionStages.WithProximityPlacementGroup;
import com.azure.resourcemanager.compute.models.VirtualMachine.DefinitionStages.WithPublicIPAddress;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.ip.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import com.microsoft.azure.toolkit.lib.compute.network.model.Subnet;
import com.microsoft.azure.toolkit.lib.compute.security.NetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AuthenticationType;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AzureSpotConfig;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class DraftVirtualMachine extends VirtualMachine implements AzureResourceDraft<VirtualMachine> {
    private Region region;
    private AzureImage image;
    private Network network;
    private Subnet subnet;
    private PublicIpAddress ipAddress;
    private NetworkSecurityGroup securityGroup;
    private String userName;
    private AuthenticationType authenticationType;
    private String password;
    private String sshKey;
    private AzureVirtualMachineSize size;
    private String availabilitySet;
    private StorageAccount storageAccount;
    private AzureSpotConfig azureSpotConfig;

    public DraftVirtualMachine(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(getResourceId(subscriptionId, resourceGroup, name), null);
    }

    public void setSubscriptionId(final String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setResourceGroup(final String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return Optional.ofNullable(remote).map(HasId::id).orElseGet(() -> getResourceId(subscriptionId, resourceGroup, name));
    }

    @Override
    protected String loadStatus() {
        return Optional.ofNullable(remote).map(ignore -> super.loadStatus()).orElse(Status.DRAFT);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.compute.models.VirtualMachine loadRemote() {
        return Optional.ofNullable(remote).map(ignore -> super.loadRemote()).orElse(null);
    }

    VirtualMachine create(final AzureVirtualMachine module) {
        this.module = module;
        final WithPublicIPAddress withPublicIPAddress = module.getVirtualMachinesManager(subscriptionId).define(this.getName())
                .withRegion(this.getRegion().getName())
                .withExistingResourceGroup(this.getResourceGroup())
                .withExistingPrimaryNetwork(this.getNetworkClient())
                .withSubnet(subnet.getName())
                .withPrimaryPrivateIPAddressDynamic();
        final WithProximityPlacementGroup withProximityPlacementGroup = ipAddress != null ?
                withPublicIPAddress.withExistingPrimaryPublicIPAddress(getPublicIpAddressClient()) : withPublicIPAddress.withoutPrimaryPublicIPAddress();
        final WithCreate withCreate = configureImage(withProximityPlacementGroup);
        if (StringUtils.isNotEmpty(availabilitySet)) {
            withCreate.withExistingAvailabilitySet(getAvailabilitySetClient());
        }
        if (storageAccount != null) {
            // todo: implement storage account
        }
        if (azureSpotConfig != null) {
            // todo: implement azure spot related configs
        }
        this.remote = withCreate.create();
        if (securityGroup != null) {
            this.remote.getPrimaryNetworkInterface().update().withExistingNetworkSecurityGroup(getSecurityGroupClient()).apply();
        }
        refreshStatus();
        module.refresh();
        return this;
    }

    private WithCreate configureImage(final WithProximityPlacementGroup withCreate) {
        if (getImage().getOperatingSystem() == OperatingSystem.Windows) {
            return withCreate.withSpecificWindowsImageVersion(image.getImageReference())
                    .withAdminUsername(userName).withAdminPassword(password).withSize(size.getName());
        } else {
            final WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged withLinuxImage =
                    withCreate.withSpecificLinuxImageVersion(image.getImageReference()).withRootUsername(userName);
            final WithLinuxCreateManagedOrUnmanaged withLinuxAuthentication = authenticationType == AuthenticationType.Password ?
                    withLinuxImage.withRootPassword(password) : withLinuxImage.withSsh(sshKey);
            return withLinuxAuthentication.withSize(size.getName());
        }
    }

    private com.azure.resourcemanager.network.models.NetworkSecurityGroup getSecurityGroupClient() {
        return Optional.ofNullable(module).map(parent -> parent.getVirtualMachinesManager(subscriptionId).manager())
                .map(manager -> manager.networkManager().networkSecurityGroups().getByResourceGroup(resourceGroup, securityGroup.name())).orElse(null);
    }

    private AvailabilitySet getAvailabilitySetClient() {
        return Optional.ofNullable(module).map(parent -> parent.getVirtualMachinesManager(subscriptionId).manager())
                .map(manager -> manager.availabilitySets().getByResourceGroup(resourceGroup, availabilitySet)).orElse(null);
    }

    private com.azure.resourcemanager.network.models.PublicIpAddress getPublicIpAddressClient() {
        return Optional.ofNullable(module).map(parent -> parent.getVirtualMachinesManager(subscriptionId).manager())
                .map(manager -> manager.networkManager().publicIpAddresses().getById(ipAddress.getId())).orElse(null);
    }

    private com.azure.resourcemanager.network.models.Network getNetworkClient() {
        return Optional.ofNullable(module).map(parent -> parent.getVirtualMachinesManager(subscriptionId).manager())
                .map(manager -> manager.networkManager().networks().getById(network.getId())).orElse(null);
    }

    private static String getResourceId(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", subscriptionId, resourceGroup, name);
    }
}
