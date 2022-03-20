/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineDraft;
import com.microsoft.azure.toolkit.lib.network.networksecuritygroup.NetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.network.networksecuritygroup.NetworkSecurityGroupDraft;
import com.microsoft.azure.toolkit.lib.network.publicipaddress.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.network.publicipaddress.PublicIpAddressDraft;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.Network;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.NetworkDraft;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreateVirtualMachineTask extends AzureTask<VirtualMachine> {
    private final VirtualMachineDraft draftVirtualMachine;
    private final List<AzureTask<?>> subTasks;
    private VirtualMachine result;

    public CreateVirtualMachineTask(final VirtualMachineDraft draftVirtualMachine) {
        this.draftVirtualMachine = draftVirtualMachine;
        this.subTasks = this.initTasks();
    }

    private List<AzureTask<?>> initTasks() {
        final List<AzureTask<?>> tasks = new ArrayList<>();
        tasks.add(new CreateResourceGroupTask(draftVirtualMachine.subscriptionId(), draftVirtualMachine.resourceGroup(), draftVirtualMachine.getRegion()));
        // Create Virtual Network
        final Network network = draftVirtualMachine.getNetwork();
        if (Objects.nonNull(network) && network.isDraft()) {
            final AzureString title = AzureString.format("Create new Virtual network({0})", network.getName());
            tasks.add(new AzureTask<>(title, () -> ((NetworkDraft) network).createIfNotExist()));
        }
        // Create Public IP
        final PublicIpAddress publicIpAddress = draftVirtualMachine.getIpAddress();
        if (Objects.nonNull(publicIpAddress) && publicIpAddress.isDraft()) {
            final AzureString title = AzureString.format("Create new Public Ip address({0})", publicIpAddress.getName());
            tasks.add(new AzureTask<>(title, () -> ((PublicIpAddressDraft) publicIpAddress).createIfNotExist()));
        }
        // Create Security Group
        final NetworkSecurityGroup securityGroup = draftVirtualMachine.getSecurityGroup();
        if (Objects.nonNull(securityGroup) && securityGroup.isDraft()) {
            final AzureString title = AzureString.format("Create Network security group ({0})", securityGroup.getName());
            tasks.add(new AzureTask<>(title, () -> ((NetworkSecurityGroupDraft) securityGroup).createIfNotExist()));
        }
        // Create Storage Account
        // todo: migrate storage account to draft style
        final StorageAccountConfig storageConfig = draftVirtualMachine.getStorageAccount();
        if (storageConfig != null && StringUtils.isEmpty(storageConfig.getId())) {
            final String message = "'subscription' is required to create a Storage account.";
            final String subscriptionId = Objects.requireNonNull(storageConfig.getSubscriptionId(), message);
            tasks.add(new CreateResourceGroupTask(subscriptionId, storageConfig.getResourceGroupName(), storageConfig.getRegion()));
            final AzureString title = AzureString.format("Create Storage account ({0})", storageConfig.getName());
            tasks.add(new AzureTask<StorageAccountConfig>(title, () -> {
                final StorageAccountDraft draft = Azure.az(AzureStorageAccount.class).forSubscription(subscriptionId)
                    .storageAccounts().create(storageConfig.getName(), storageConfig.getResourceGroupName());
                draft.setConfig(storageConfig);
                final StorageAccount result = draft.commit();
                storageConfig.setId(result.getId());
            }));
        }
        // Create VM
        final AzureString title = AzureString.format("Create Virtual machine ({0})", draftVirtualMachine.getName());
        tasks.add(new AzureTask<>(title, () -> {
            CreateVirtualMachineTask.this.result = draftVirtualMachine.createIfNotExist();
            return CreateVirtualMachineTask.this.result;
        }));
        return tasks;
    }

    public VirtualMachine doExecute() throws Exception {
        for (final AzureTask<?> t : this.subTasks) {
            t.getBody().call();
        }
        return this.result;
    }
}
