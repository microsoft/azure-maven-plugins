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
import com.azure.resourcemanager.compute.models.VirtualMachineEvictionPolicyTypes;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.ip.DraftPublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.ip.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.network.DraftNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import com.microsoft.azure.toolkit.lib.compute.network.model.Subnet;
import com.microsoft.azure.toolkit.lib.compute.security.DraftNetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.NetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.model.SecurityRule;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AuthenticationType;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AzureSpotConfig;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.storage.StorageManagerFactory;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

import static com.azure.resourcemanager.compute.models.VirtualMachineEvictionPolicyTypes.DEALLOCATE;
import static com.azure.resourcemanager.compute.models.VirtualMachineEvictionPolicyTypes.DELETE;
import static com.microsoft.azure.toolkit.lib.compute.vm.model.AzureSpotConfig.EvictionPolicy.StopAndDeallocate;

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
    private StorageAccountConfig storageAccount;
    private AzureSpotConfig azureSpotConfig;

    public DraftVirtualMachine(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(getResourceId(subscriptionId, resourceGroup, name), null);
    }

    public static DraftVirtualMachine getDefaultVirtualMachineDraft() {
        final DraftVirtualMachine virtualMachine = new DraftVirtualMachine();
        virtualMachine.setRegion(Region.US_CENTRAL);
        virtualMachine.setImage(AzureImage.UBUNTU_SERVER_18_04_LTS);
        virtualMachine.setSize(AzureVirtualMachineSize.Standard_D2s_v3);
        virtualMachine.setNetwork(DraftNetwork.getDefaultNetworkDraft());
        virtualMachine.setIpAddress(DraftPublicIpAddress.getDefaultPublicIpAddressDraft());
        final DraftNetworkSecurityGroup defaultSecurityGroup = new DraftNetworkSecurityGroup();
        defaultSecurityGroup.setSecurityRuleList(Arrays.asList(SecurityRule.SSH_RULE));
        virtualMachine.setSecurityGroup(defaultSecurityGroup);
        return virtualMachine;
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
    public String status() {
        return Optional.ofNullable(remote).map(ignore -> super.status()).orElse(Status.DRAFT);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.compute.models.VirtualMachine loadRemote() {
        return Optional.ofNullable(remote).map(ignore -> super.loadRemote()).orElse(null);
    }

    VirtualMachine create(final AzureVirtualMachine module) {
        this.module = module;
        final NetworkInterface.DefinitionStages.WithCreate interfaceWithCreate = module.getVirtualMachinesManager(subscriptionId).manager().networkManager()
                .networkInterfaces().define(name + "-interface-" + Utils.getTimestamp())
                .withRegion(this.getRegion().getName())
                .withExistingResourceGroup(this.getResourceGroup())
                .withExistingPrimaryNetwork(this.getNetworkClient())
                .withSubnet(subnet.getName())
                .withPrimaryPrivateIPAddressDynamic();
        if (ipAddress != null) {
            final com.azure.resourcemanager.network.models.PublicIpAddress publicIpAddressClient = getPublicIpAddressClient();
            if (publicIpAddressClient.hasAssignedNetworkInterface()) {
                AzureMessager.getMessager().warning(AzureString.format("Can not assign public ip %s to vm %s, which has been assigned to %s",
                        ipAddress.getName(), name, publicIpAddressClient.getAssignedNetworkInterfaceIPConfiguration().name()));
            } else {
                interfaceWithCreate.withExistingPrimaryPublicIPAddress(getPublicIpAddressClient());
            }
        }
        if (securityGroup != null) {
            interfaceWithCreate.withExistingNetworkSecurityGroup(getSecurityGroupClient());
        }
        final NetworkInterface networkInterface = interfaceWithCreate.create();
        final WithProximityPlacementGroup withProximityPlacementGroup = module.getVirtualMachinesManager(subscriptionId).define(this.getName())
                .withRegion(this.getRegion().getName())
                .withExistingResourceGroup(this.getResourceGroup())
                .withExistingPrimaryNetworkInterface(networkInterface);
        final WithCreate withCreate = configureImage(withProximityPlacementGroup);
        if (StringUtils.isNotEmpty(availabilitySet)) {
            withCreate.withExistingAvailabilitySet(getAvailabilitySetClient());
        }
        if (storageAccount != null) {
            withCreate.withExistingStorageAccount(getStorageAccountClient());
        }
        if (azureSpotConfig != null) {
            final VirtualMachineEvictionPolicyTypes evictionPolicyTypes = azureSpotConfig.getPolicy() == StopAndDeallocate ? DEALLOCATE : DELETE;
            withCreate.withSpotPriority(evictionPolicyTypes).withMaxPrice(azureSpotConfig.getMaximumPrice());
        }
        try {
            this.remote = withCreate.create();
        } catch (Exception e) {
            // clean up resource once creation failed
            networkInterface.manager().networkInterfaces().deleteById(networkInterface.id());
            throw e;
        }
        refreshStatus();
        module.refresh();
        return this;
    }

    private StorageAccount getStorageAccountClient() {
        return StorageManagerFactory.create(subscriptionId).storageAccounts().getById(storageAccount.getId());
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
