/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineModule;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AzureCompute extends AbstractAzService<ComputeResourceManager, ComputeManager> {
    public AzureCompute() {
        super("Microsoft.Network");
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
        return "Azure Network";
    }
}
