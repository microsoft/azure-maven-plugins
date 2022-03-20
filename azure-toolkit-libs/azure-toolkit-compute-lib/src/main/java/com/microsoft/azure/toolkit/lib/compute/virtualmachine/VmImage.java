/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.ImageReference;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.KnownWindowsVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachineImage;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.model.OperatingSystem;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;

@EqualsAndHashCode
public class VmImage {
    public static final VmImage WINDOWS_DESKTOP_10_20H1_PRO = new VmImage(KnownWindowsVirtualMachineImage.WINDOWS_DESKTOP_10_20H1_PRO);
    public static final VmImage WINDOWS_SERVER_2019_DATACENTER = new VmImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2019_DATACENTER);
    public static final VmImage WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINER =
            new VmImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS);
    public static final VmImage WINDOWS_SERVER_2016_DATACENTER = new VmImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2016_DATACENTER);
    public static final VmImage WINDOWS_SERVER_2012_R2_DATACENTER = new VmImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER);
    public static final VmImage UBUNTU_SERVER_16_04_LTS = new VmImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS);
    public static final VmImage UBUNTU_SERVER_18_04_LTS = new VmImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_18_04_LTS);
    public static final VmImage DEBIAN_9 = new VmImage(KnownLinuxVirtualMachineImage.DEBIAN_9);
    public static final VmImage DEBIAN_10 = new VmImage(KnownLinuxVirtualMachineImage.DEBIAN_10);
    public static final VmImage CENTOS_8_1 = new VmImage(KnownLinuxVirtualMachineImage.CENTOS_8_1);
    public static final VmImage CENTOS_8_3 = new VmImage(KnownLinuxVirtualMachineImage.CENTOS_8_3);
    @Deprecated
    public static final VmImage OPENSUSE_LEAP_15_1 = new VmImage(KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_15_1);
    public static final VmImage OPENSUSE_LEAP_15 = new VmImage(KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_15);
    @Deprecated
    public static final VmImage SLES_15_SP1 = new VmImage(KnownLinuxVirtualMachineImage.SLES_15_SP1);
    public static final VmImage SLES_15 = new VmImage(KnownLinuxVirtualMachineImage.SLES_15);
    public static final VmImage REDHAT_RHEL_8_2 = new VmImage(KnownLinuxVirtualMachineImage.REDHAT_RHEL_8_2);
    public static final VmImage ORACLE_LINUX_8_1 = new VmImage(KnownLinuxVirtualMachineImage.ORACLE_LINUX_8_1);

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

    public VmImage(@Nonnull VirtualMachineImage virtualMachineImage) {
        this(OperatingSystem.fromString(virtualMachineImage.osDiskImage().operatingSystem().name()), virtualMachineImage.imageReference());
    }

    public VmImage(@Nonnull KnownWindowsVirtualMachineImage windowsVirtualMachineImage) {
        this(OperatingSystem.Windows, windowsVirtualMachineImage.imageReference());
    }

    public VmImage(@Nonnull KnownLinuxVirtualMachineImage linuxVirtualMachineImage) {
        this(OperatingSystem.Linux, linuxVirtualMachineImage.imageReference());
    }

    public VmImage(final OperatingSystem operatingSystem, final ImageReference imageReference) {
        this.operatingSystem = operatingSystem;
        this.id = imageReference.id();
        this.publisherName = imageReference.publisher();
        this.offer = imageReference.offer();
        this.sku = imageReference.sku();
        this.version = imageReference.version();

        this.imageReference = imageReference;
    }
}
