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
    @EqualsAndHashCode.Exclude
    private final ImageReference imageReference;

    @Nonnull
    @Getter
    private final OperatingSystem operatingSystem;
    @Getter
    private final String id;
    @Getter
    private final String publisherName;
    @Getter
    private final String offer;
    @Getter
    private final String sku;
    @Getter
    private final String version;

    public AzureImage(@Nonnull VirtualMachineImage virtualMachineImage) {
        this(OperatingSystem.fromString(virtualMachineImage.osDiskImage().operatingSystem().name()), virtualMachineImage.imageReference());
    }

    public AzureImage(@Nonnull KnownWindowsVirtualMachineImage windowsVirtualMachineImage) {
        this(OperatingSystem.Windows, windowsVirtualMachineImage.imageReference());
    }

    public AzureImage(@Nonnull KnownLinuxVirtualMachineImage linuxVirtualMachineImage) {
        this(OperatingSystem.Linux, linuxVirtualMachineImage.imageReference());
    }

    public AzureImage(final OperatingSystem operatingSystem, final ImageReference imageReference) {
        this.operatingSystem = operatingSystem;
        this.id = imageReference.id();
        this.publisherName = imageReference.publisher();
        this.offer = imageReference.offer();
        this.sku = imageReference.sku();
        this.version = imageReference.version();

        this.imageReference = imageReference;
    }
}
