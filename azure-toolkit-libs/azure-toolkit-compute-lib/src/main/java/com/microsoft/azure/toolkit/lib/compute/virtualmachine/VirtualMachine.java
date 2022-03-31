/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.LinuxConfiguration;
import com.azure.resourcemanager.compute.models.OSProfile;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.compute.ComputeResourceManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.model.OperatingSystem;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VirtualMachine extends AbstractAzResource<VirtualMachine, ComputeResourceManager, com.azure.resourcemanager.compute.models.VirtualMachine>
    implements Startable, Deletable {

    protected VirtualMachine(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull VirtualMachineModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected VirtualMachine(@Nonnull VirtualMachine origin) {
        super(origin);
    }

    protected VirtualMachine(@Nonnull com.azure.resourcemanager.compute.models.VirtualMachine remote, @Nonnull VirtualMachineModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, VirtualMachine, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.compute.models.VirtualMachine remote) {
        return StringUtils.capitalize(remote.powerState().toString().substring("PowerState/".length()));
    }

    @AzureOperation(name = "vm.start.vm", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).start(), Status.STARTING);
    }

    @AzureOperation(name = "vm.stop.vm", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).powerOff(), Status.STOPPING);
    }

    @AzureOperation(name = "vm.restart.vm", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).restart(), Status.RESTARTING);
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    public OperatingSystem getOperatingSystem() {
        return this.remoteOptional()
            .map(com.azure.resourcemanager.compute.models.VirtualMachine::osProfile)
            .map(OSProfile::windowsConfiguration)
            .map(ignore -> OperatingSystem.Windows)
            .orElse(OperatingSystem.Linux);
    }

    public boolean isSshEnabled() {
        // TODO: @wangmi check if ssh is enabled, possible solution: INBOUND/TCP/22
        // return remote().getPrimaryNetworkInterface().getNetworkSecurityGroup().securityRules().entrySet().stream()
        //    .anyMatch(e -> SecurityRuleProtocol.TCP.equals(e.getValue().protocol()) &&
        //        SecurityRuleDirection.INBOUND.equals(e.getValue().direction()) &&
        //        "22".equals(e.getValue().destinationPortRange()) &&
        //        "*".equals(e.getValue().destinationPortRange()));
        return Objects.nonNull(this.getHostIp());
    }

    @Nullable
    public String getHostIp() {
        return this.remoteOptional()
            .map(com.azure.resourcemanager.compute.models.VirtualMachine::getPrimaryPublicIPAddress)
            .map(PublicIpAddress::ipAddress)
            .orElse(null);
    }

    public String getAdminUserName() {
        return this.remoteOptional()
            .map(com.azure.resourcemanager.compute.models.VirtualMachine::osProfile)
            .map(OSProfile::adminUsername)
            .orElse(null);
    }

    public boolean isPasswordAuthenticationDisabled() {
        return this.remoteOptional()
            .map(com.azure.resourcemanager.compute.models.VirtualMachine::osProfile)
            .map(OSProfile::linuxConfiguration)
            .map(LinuxConfiguration::disablePasswordAuthentication)
            .orElse(false);
    }
}
