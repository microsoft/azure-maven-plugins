/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.containerservice;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.containerservice.model.AgentPoolMode;
import com.microsoft.azure.toolkit.lib.containerservice.model.OsType;
import com.microsoft.azure.toolkit.lib.containerservice.model.PowerState;
import com.microsoft.azure.toolkit.lib.containerservice.model.VirtualMachineSize;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KubernetesClusterAgentPool extends AbstractAzResource<KubernetesClusterAgentPool, KubernetesCluster,
        com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool> implements Deletable {

    protected KubernetesClusterAgentPool(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KubernetesClusterAgentPoolModule module) {
        super(name, resourceGroupName, module);
    }

    public KubernetesClusterAgentPool(@Nonnull com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool remote,
                                      @Nonnull KubernetesClusterAgentPoolModule module) {
        super(remote.name(), module);
        this.setRemote(remote);
    }

    public int getNodeCount() {
        return Optional.ofNullable(getRemote()).map(pool -> pool.count()).orElse(0);
    }

    @Nullable
    public PowerState getPowerStatus() {
        return Optional.ofNullable(getRemote()).map(pool -> PowerState.fromString(pool.powerState().code().toString())).orElse(null);
    }

    @Nullable
    public AgentPoolMode getAgentPoolMode() {
        return Optional.ofNullable(getRemote()).map(pool -> pool.mode()).map(mode -> AgentPoolMode.fromString(mode.toString())).orElse(null);
    }

    @Nullable
    public String getKubernetesVersion() {
        return Optional.ofNullable(getRemote()).map(pool -> pool.innerModel().orchestratorVersion()).orElse(null);
    }

    @Nullable
    public VirtualMachineSize getVirtualMachineSize() {
        return Optional.ofNullable(getRemote()).map(pool -> pool.vmSize()).map(size -> VirtualMachineSize.fromString(size.toString())).orElse(null);
    }

    @Nullable
    public OsType getOsType() {
        return Optional.ofNullable(getRemote()).map(pool -> pool.osType()).map(os -> OsType.fromString(os.toString())).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, KubernetesClusterAgentPool, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool remote) {
        final String provisioningState = remote.provisioningState();
        return StringUtils.equalsIgnoreCase("Succeeded", provisioningState) ?
                Optional.ofNullable(getPowerStatus()).map(PowerState::getValue).orElse(Status.UNKNOWN) :
                provisioningState;
    }
}
