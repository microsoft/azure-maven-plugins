/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm;

import com.azure.resourcemanager.compute.models.VirtualMachinePublisher;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureImagePublisher {
    private final VirtualMachinePublisher virtualMachinePublisher;

    AzureImagePublisher(@Nonnull VirtualMachinePublisher virtualMachinePublisher) {
        this.virtualMachinePublisher = virtualMachinePublisher;
    }

    public String name() {
        return virtualMachinePublisher.name();
    }

    public Region region() {
        return Region.fromName(virtualMachinePublisher.region().name());
    }

    public List<AzureImageOffer> offers() {
        return virtualMachinePublisher.offers().list().stream().map(offer -> new AzureImageOffer(this, offer)).collect(Collectors.toList());
    }
}
