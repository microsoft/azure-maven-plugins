/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.VirtualMachineOffer;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class VmImageOffer {
    @Getter
    private final VmImagePublisher publisher;
    private final VirtualMachineOffer virtualMachineOffer;

    VmImageOffer(@Nonnull VmImagePublisher publisher, @Nonnull VirtualMachineOffer virtualMachineOffer) {
        this.publisher = publisher;
        this.virtualMachineOffer = virtualMachineOffer;
    }

    public String name() {
        return virtualMachineOffer.name();
    }

    public Region region() {
        return Region.fromName(virtualMachineOffer.region().name());
    }

    public List<VmImageSku> skus() {
        return virtualMachineOffer.skus().list().stream().map(sku -> new VmImageSku(this, sku)).collect(Collectors.toList());
    }
}
