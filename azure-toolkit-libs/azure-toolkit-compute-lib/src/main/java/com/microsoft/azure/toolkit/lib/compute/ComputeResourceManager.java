/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class ComputeResourceManager extends AbstractAzResourceManager<ComputeResourceManager, ComputeManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final VirtualMachineModule virtualMachineModule;

    ComputeResourceManager(@Nonnull String subscriptionId, @Nonnull AzureCompute service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.virtualMachineModule = new VirtualMachineModule(this);
    }

    ComputeResourceManager(@Nonnull ComputeManager remote, @Nonnull AzureCompute service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, ComputeResourceManager, ?>> getSubModules() {
        return Collections.singletonList(virtualMachineModule);
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }
}

