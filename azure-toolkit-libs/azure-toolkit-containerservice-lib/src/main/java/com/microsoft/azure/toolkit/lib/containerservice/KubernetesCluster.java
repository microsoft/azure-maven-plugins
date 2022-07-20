/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.containerservice.model.ContainerServiceNetworkProfile;
import com.microsoft.azure.toolkit.lib.containerservice.model.PowerState;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KubernetesCluster extends AbstractAzResource<KubernetesCluster, ContainerServiceSubscription,
        com.azure.resourcemanager.containerservice.models.KubernetesCluster> implements Startable, Deletable {

    private KubernetesClusterAgentPoolModule agentPoolModule;

    protected KubernetesCluster(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KubernetesClusterModule module) {
        super(name, resourceGroupName, module);
        this.agentPoolModule = new KubernetesClusterAgentPoolModule(this);
    }

    protected KubernetesCluster(@Nonnull KubernetesCluster cluster) {
        super(cluster);
        this.agentPoolModule = new KubernetesClusterAgentPoolModule(this);
    }

    protected KubernetesCluster(@Nonnull com.azure.resourcemanager.containerservice.models.KubernetesCluster remote, @Nonnull KubernetesClusterModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.setRemote(remote);
        this.agentPoolModule = new KubernetesClusterAgentPoolModule(this);
    }

    public ContainerServiceNetworkProfile getContainerServiceNetworkProfile() {
        return Optional.ofNullable(getRemote()).map(cluster -> ContainerServiceNetworkProfile.fromNetworkProfile(cluster.networkProfile())).orElse(null);
    }

    public String getVersion() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.version()).orElse(null);
    }

    public String getApiServerAddress() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.innerModel().fqdn()).orElse(null);
    }

    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.region()).map(region -> Region.fromName(region.name())).orElse(null);
    }

    public PowerState getPowerStatus() {
        return Optional.ofNullable(getRemote()).map(remote -> PowerState.fromString(remote.powerState().code().toString())).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, KubernetesCluster, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.containerservice.models.KubernetesCluster remote) {
        final String provisioningState = remote.provisioningState();
        return StringUtils.equalsIgnoreCase("Succeeded", provisioningState) ? getPowerStatus().getValue() : provisioningState;
    }

    @Override
    public void start() {
        getRemote().start();
    }

    @Override
    public void stop() {
        getRemote().stop();
    }

    public byte[] getAdminKubeConfig() {
        return getRemote().adminKubeConfigContent();
    }

    public byte[] getUserKubeConfig() {
        return getRemote().userKubeConfigContent();
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isRestartable() {
        return false;
    }

    public KubernetesClusterAgentPoolModule agentPools() {
        return agentPoolModule;
    }
}
