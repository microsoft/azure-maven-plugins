/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.LinuxConfiguration;
import com.azure.resourcemanager.compute.models.OSProfile;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.compute.ComputeServiceSubscription;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.network.networksecuritygroup.SecurityRule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VirtualMachine extends AbstractAzResource<VirtualMachine, ComputeServiceSubscription, com.azure.resourcemanager.compute.models.VirtualMachine>
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
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull com.azure.resourcemanager.compute.models.VirtualMachine remote) {
        final String provisioningState = remote.provisioningState();
        return Optional.ofNullable(provisioningState).filter(s -> s.equalsIgnoreCase("succeeded"))
            .map(i -> remote.powerState())
            .map(s -> s.toString().substring("PowerState/".length()))
            .map(StringUtils::capitalize)
            .orElse(StringUtils.firstNonBlank(provisioningState, Status.UNKNOWN));
    }

    @AzureOperation(name = "azure/vm.start_vm.vm", params = {"this.getName()"})
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).start(), Status.STARTING);
    }

    @AzureOperation(name = "azure/vm.stop_vm.vm", params = {"this.getName()"})
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).powerOff(), Status.STOPPING);
    }

    @AzureOperation(name = "azure/vm.restart_vm.vm", params = {"this.getName()"})
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
    @Cacheable(cacheName = "vm/{}/hostIp", key = "${this.getId()}")
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

    public int getSshPort() {
        return SecurityRule.SSH_RULE.getToPort();
    }
}
