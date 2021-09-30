/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.compute.ip.AzurePublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.ip.DraftPublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.ip.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.DraftNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import com.microsoft.azure.toolkit.lib.compute.security.AzureNetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.DraftNetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.NetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.DraftVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.VirtualMachine;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CreateVirtualMachineTask extends AzureTask<VirtualMachine> {
    private final DraftVirtualMachine draftVirtualMachine;
    private final List<AzureTask<?>> subTasks;
    private VirtualMachine result;

    public CreateVirtualMachineTask(final DraftVirtualMachine draftVirtualMachine) {
        this.draftVirtualMachine = draftVirtualMachine;
        this.subTasks = this.initTasks();
    }

    private List<AzureTask<?>> initTasks() {
        final List<AzureTask<?>> tasks = new ArrayList<>();
        tasks.add(new CreateResourceGroupTask(draftVirtualMachine.subscriptionId(), draftVirtualMachine.resourceGroup(), draftVirtualMachine.getRegion()));
        // Create Virtual Network
        final Network network = draftVirtualMachine.getNetwork();
        if (network instanceof DraftNetwork && StringUtils.equalsIgnoreCase(network.status(), IAzureBaseResource.Status.DRAFT)) {
            final AzureString title = AzureString.format("Create new virtual network({0})", network.getName());
            tasks.add(new AzureTask<>(title, () -> Azure.az(AzureNetwork.class).create((DraftNetwork) network)));
        }
        // Create Public IP
        final PublicIpAddress publicIpAddress = draftVirtualMachine.getIpAddress();
        if (publicIpAddress instanceof DraftPublicIpAddress && StringUtils.equalsIgnoreCase(publicIpAddress.status(), IAzureBaseResource.Status.DRAFT)) {
            final AzureString title = AzureString.format("Create new public ip address({0})", publicIpAddress.getName());
            tasks.add(new AzureTask<>(title, () -> Azure.az(AzurePublicIpAddress.class).create((DraftPublicIpAddress) publicIpAddress)));
        }
        // Create Security Group
        final NetworkSecurityGroup securityGroup = draftVirtualMachine.getSecurityGroup();
        if (securityGroup instanceof DraftNetworkSecurityGroup && StringUtils.equalsIgnoreCase(securityGroup.status(), IAzureBaseResource.Status.DRAFT)) {
            final AzureString title = AzureString.format("Create security group ({0})", securityGroup.getName());
            tasks.add(new AzureTask<DraftNetworkSecurityGroup>(title, () ->
                    Azure.az(AzureNetworkSecurityGroup.class).create((DraftNetworkSecurityGroup) securityGroup)));
        }
        // Create Storage Account
        // todo: migrate storage account to draft style
        final StorageAccountConfig storageAccount = draftVirtualMachine.getStorageAccount();
        if (storageAccount != null && StringUtils.isEmpty(storageAccount.getId())) {
            tasks.add(new CreateResourceGroupTask(storageAccount.getSubscriptionId(), storageAccount.getResourceGroupName(), storageAccount.getRegion()));
            final AzureString title = AzureString.format("Create storage account ({0})", storageAccount.getName());
            tasks.add(new AzureTask<StorageAccountConfig>(title, () -> {
                final StorageAccount result = Azure.az(AzureStorageAccount.class).create(storageAccount).commit();
                storageAccount.setId(result.id());
            }));
        }
        // Create VM
        final AzureString title = AzureString.format("Create virtual machine ({0})", draftVirtualMachine.getName());
        tasks.add(new AzureTask<>(title, () -> {
            CreateVirtualMachineTask.this.result = Azure.az(AzureVirtualMachine.class).create(draftVirtualMachine);
            return CreateVirtualMachineTask.this.result;
        }));
        return tasks;
    }

    public VirtualMachine execute() {
        this.subTasks.forEach(t -> t.getSupplier().get());
        return this.result;
    }
}
