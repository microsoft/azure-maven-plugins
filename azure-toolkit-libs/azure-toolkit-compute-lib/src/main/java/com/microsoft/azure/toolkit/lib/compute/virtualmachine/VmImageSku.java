/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.VirtualMachineSku;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class VmImageSku {
    @Getter
    private final VmImageOffer publisher;
    private final VirtualMachineSku virtualMachineSku;

    VmImageSku(@Nonnull VmImageOffer imageOffer, @Nonnull VirtualMachineSku virtualMachineSku) {
        this.publisher = imageOffer;
        this.virtualMachineSku = virtualMachineSku;
    }

    public String name() {
        return virtualMachineSku.name();
    }

    public Region region() {
        return Region.fromName(virtualMachineSku.region().name());
    }

    public List<VmImage> images() {
        return virtualMachineSku.images().list().stream().map(VmImage::new).collect(Collectors.toList());
    }
}
