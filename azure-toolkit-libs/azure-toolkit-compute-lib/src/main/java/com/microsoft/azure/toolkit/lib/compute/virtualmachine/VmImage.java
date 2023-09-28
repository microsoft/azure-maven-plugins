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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class VmImage {
    public static final VmImage WINDOWS_DESKTOP_11_22H2_PRO = new VmImage(OperatingSystem.Windows,
        new ImageReference().withPublisher("MicrosoftWindowsDesktop").withOffer("windows-11").withSku("win11-22h2-pro").withVersion("latest"), "Windows 11 Pro, version 22H2 - x64 Gen2");
    public static final VmImage WINDOWS_DESKTOP_10_22H2_PRO = new VmImage(OperatingSystem.Windows,
        new ImageReference().withPublisher("MicrosoftWindowsDesktop").withOffer("Windows-10").withSku("win10-22h2-pro").withVersion("latest"), "Windows 10 Pro, version 22H2 - x64 Gen2");
    public static final VmImage WINDOWS_SERVER_2022_DATACENTER = new VmImage(OperatingSystem.Windows,
        new ImageReference().withPublisher("MicrosoftWindowsServer").withOffer("WindowsServer").withSku("2022-datacenter-azure-edition").withVersion("latest"), "Windows Server 2022 Datacenter: Azure Edition Hotpatch - x64 Gen2");
    public static final VmImage WINDOWS_SERVER_2022_DATACENTER_HOT_PATCH = new VmImage(OperatingSystem.Windows,
        new ImageReference().withPublisher("MicrosoftWindowsServer").withOffer("WindowsServer").withSku("2022-datacenter-azure-edition-hotpatch").withVersion("latest"), "Windows Server 2022 Datacenter: Azure Edition - x64 Gen2");
    public static final VmImage WINDOWS_SERVER_2019_DATACENTER = new VmImage(OperatingSystem.Windows,
        new ImageReference().withPublisher("MicrosoftWindowsDesktop").withOffer("WindowsServer").withSku("2019-Datacenter").withVersion("latest"), "Windows Server 2019 Datacenter - x64 Gen2");
    public static final VmImage WINDOWS_SERVER_2016_DATACENTER = new VmImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2016_DATACENTER);
    public static final List<VmImage> WINDOWS_IMAGES = Collections.unmodifiableList(Arrays.asList(WINDOWS_DESKTOP_11_22H2_PRO, WINDOWS_DESKTOP_10_22H2_PRO,
        WINDOWS_SERVER_2022_DATACENTER_HOT_PATCH, WINDOWS_SERVER_2022_DATACENTER, WINDOWS_SERVER_2019_DATACENTER, WINDOWS_SERVER_2016_DATACENTER));

    public static final VmImage UBUNTU_SERVER_22_04_LTS = new VmImage(OperatingSystem.Linux,
        new ImageReference().withPublisher("Canonical").withOffer("0001-com-ubuntu-server-jammy").withSku("22_04-lts").withVersion("latest"), "Ubuntu Server 22.04 LTS Gen2");
    public static final VmImage UBUNTU_SERVER_20_04_LTS = new VmImage(OperatingSystem.Linux,
        new ImageReference().withPublisher("Canonical").withOffer("0001-com-ubuntu-server-focal").withSku("20_04-lts").withVersion("latest"), "Ubuntu Server 20.04 LTS Gen2");
    public static final VmImage OPENSUSE_LEAP_15_4 = new VmImage(OperatingSystem.Linux,
        new ImageReference().withPublisher("SUSE").withOffer("openSUSE-Leap-15-4").withSku("gen2").withVersion("latest"), "SUSE Linux Enterprise Server 15 SP4 Gen2");
    public static final VmImage REDHAT_RHEL_8_7 = new VmImage(OperatingSystem.Linux,
        new ImageReference().withPublisher("RedHat").withOffer("RHEL").withSku("87-gen2").withVersion("latest"), "Red Hat Enterprise Linux 8.7 (LVM) Gen2");
    public static final VmImage ORACLE_LINUX_8_6 = new VmImage(OperatingSystem.Linux,
        new ImageReference().withPublisher("Oracle").withOffer("Oracle-Linux").withSku("ol86-lvm-gen2").withVersion("latest"), "Oracle Linux 8.6 (LVM) Gen2");
    public static final VmImage DEBIAN_11 = new VmImage(OperatingSystem.Linux,
        new ImageReference().withPublisher("Debian").withOffer("debian-11").withSku("11-gen2").withVersion("latest"), "Debian 11 \"Bullseye\" Gen2");
    public static final List<VmImage> LINUX_IMAGES = Collections.unmodifiableList(Arrays.asList(UBUNTU_SERVER_22_04_LTS, UBUNTU_SERVER_20_04_LTS,
        OPENSUSE_LEAP_15_4, REDHAT_RHEL_8_7, ORACLE_LINUX_8_6, DEBIAN_11));
    public static final List<VmImage> IMAGES =
        Collections.unmodifiableList(ListUtils.union(LINUX_IMAGES, WINDOWS_IMAGES));

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
    @Getter
    private final String displayName;

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
        this(operatingSystem, imageReference, null);
    }

    public VmImage(final @Nonnull OperatingSystem operatingSystem, final ImageReference imageReference, @Nullable final String displayName) {
        this.operatingSystem = operatingSystem;
        this.id = imageReference.id();
        this.publisherName = imageReference.publisher();
        this.offer = imageReference.offer();
        this.sku = imageReference.sku();
        this.version = imageReference.version();
        this.displayName = Optional.ofNullable(displayName).orElseGet(() -> String.format("%s %s", getOffer(), getSku()));

        this.imageReference = imageReference;
    }
}
