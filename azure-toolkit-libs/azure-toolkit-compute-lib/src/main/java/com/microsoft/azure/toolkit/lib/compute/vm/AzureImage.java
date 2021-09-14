/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm;

import com.azure.resourcemanager.compute.models.ImageReference;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.KnownWindowsVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachineImage;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;

@EqualsAndHashCode
public class AzureImage {
    @Nonnull
    @Getter(value = AccessLevel.PACKAGE)
    private final ImageReference imageReference;
    @Nonnull
    @Getter
    private final OperatingSystem operatingSystem;

    AzureImage(@Nonnull VirtualMachineImage virtualMachineImage) {
        this.operatingSystem = OperatingSystem.fromString(virtualMachineImage.osDiskImage().operatingSystem().name());
        this.imageReference = virtualMachineImage.imageReference();
    }

    public AzureImage(@Nonnull KnownWindowsVirtualMachineImage windowsVirtualMachineImage) {
        this.operatingSystem = OperatingSystem.Windows;
        this.imageReference = windowsVirtualMachineImage.imageReference();
    }

    AzureImage(@Nonnull KnownLinuxVirtualMachineImage linuxVirtualMachineImage) {
        this.operatingSystem = OperatingSystem.Linux;
        this.imageReference = linuxVirtualMachineImage.imageReference();
    }

    public String id() {
        return imageReference.id();
    }

    public String publisherName() {
        return imageReference.publisher();
    }

    public String offer() {
        return imageReference.offer();
    }

    public String sku() {
        return imageReference.sku();
    }

    public String version() {
        return imageReference.version();
    }
}
