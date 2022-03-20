/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.VirtualMachinePublisher;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class VmImagePublisher {
    private final VirtualMachinePublisher virtualMachinePublisher;

    public VmImagePublisher(@Nonnull VirtualMachinePublisher virtualMachinePublisher) {
        this.virtualMachinePublisher = virtualMachinePublisher;
    }

    public String name() {
        return virtualMachinePublisher.name();
    }

    public Region region() {
        return Region.fromName(virtualMachinePublisher.region().name());
    }

    public List<VmImageOffer> offers() {
        return virtualMachinePublisher.offers().list().stream().map(offer -> new VmImageOffer(this, offer)).collect(Collectors.toList());
    }
}
