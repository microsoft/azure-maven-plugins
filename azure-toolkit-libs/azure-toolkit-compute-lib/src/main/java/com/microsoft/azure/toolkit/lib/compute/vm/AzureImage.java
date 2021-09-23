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
    public static final AzureImage WINDOWS_DESKTOP_10_20H1_PRO = new AzureImage(KnownWindowsVirtualMachineImage.WINDOWS_DESKTOP_10_20H1_PRO);
    public static final AzureImage WINDOWS_SERVER_2019_DATACENTER = new AzureImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2019_DATACENTER);
    public static final AzureImage WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINER =
            new AzureImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS);
    public static final AzureImage WINDOWS_SERVER_2016_DATACENTER = new AzureImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2016_DATACENTER);
    public static final AzureImage WINDOWS_SERVER_2012_R2_DATACENTER = new AzureImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER);
    public static final AzureImage UBUNTU_SERVER_16_04_LTS = new AzureImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS);
    public static final AzureImage UBUNTU_SERVER_18_04_LTS = new AzureImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_18_04_LTS);
    public static final AzureImage DEBIAN_9 = new AzureImage(KnownLinuxVirtualMachineImage.DEBIAN_9);
    public static final AzureImage DEBIAN_10 = new AzureImage(KnownLinuxVirtualMachineImage.DEBIAN_10);
    public static final AzureImage CENTOS_8_1 = new AzureImage(KnownLinuxVirtualMachineImage.CENTOS_8_1);
    public static final AzureImage CENTOS_8_3 = new AzureImage(KnownLinuxVirtualMachineImage.CENTOS_8_3);
    @Deprecated
    public static final AzureImage OPENSUSE_LEAP_15_1 = new AzureImage(KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_15_1);
    public static final AzureImage OPENSUSE_LEAP_15 = new AzureImage(KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_15);
    @Deprecated
    public static final AzureImage SLES_15_SP1 = new AzureImage(KnownLinuxVirtualMachineImage.SLES_15_SP1);
    public static final AzureImage SLES_15 = new AzureImage(KnownLinuxVirtualMachineImage.SLES_15);
    public static final AzureImage REDHAT_RHEL_8_2 = new AzureImage(KnownLinuxVirtualMachineImage.REDHAT_RHEL_8_2);
    public static final AzureImage ORACLE_LINUX_8_1 = new AzureImage(KnownLinuxVirtualMachineImage.ORACLE_LINUX_8_1);


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
