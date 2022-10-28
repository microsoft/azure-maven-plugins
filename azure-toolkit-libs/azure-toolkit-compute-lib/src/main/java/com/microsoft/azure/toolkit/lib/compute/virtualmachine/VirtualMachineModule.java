/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.VirtualMachines;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.compute.ComputeServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class VirtualMachineModule extends AbstractAzResourceModule<VirtualMachine, ComputeServiceSubscription, com.azure.resourcemanager.compute.models.VirtualMachine> {

    public static final String NAME = "virtualMachines";

    public VirtualMachineModule(@Nonnull ComputeServiceSubscription parent) {
        super(NAME, parent);
    }

    @Override
    public VirtualMachines getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ComputeManager::virtualMachines).orElse(null);
    }

    @Nonnull
    @Override
    protected VirtualMachineDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new VirtualMachineDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected VirtualMachineDraft newDraftForUpdate(@Nonnull VirtualMachine origin) {
        return new VirtualMachineDraft(origin);
    }

    @Nonnull
    @Override
    protected VirtualMachine newResource(@Nonnull com.azure.resourcemanager.compute.models.VirtualMachine r) {
        return new VirtualMachine(r, this);
    }

    @Nonnull
    @Override
    protected VirtualMachine newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new VirtualMachine(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Virtual machine";
    }
}
