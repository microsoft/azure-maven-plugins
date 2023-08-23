/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.containerservice.model.ContainerServiceNetworkProfile;
import com.microsoft.azure.toolkit.lib.containerservice.model.PowerState;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KubernetesCluster extends AbstractAzResource<KubernetesCluster, ContainerServiceSubscription,
        com.azure.resourcemanager.containerservice.models.KubernetesCluster> implements Startable, Deletable {
    public static final Action.Id<KubernetesCluster> DOWNLOAD_CONFIG_ADMIN = Action.Id.of("user/kubernetes.kubu_config_admin.kubernetes");
    public static final Action.Id<KubernetesCluster> DOWNLOAD_CONFIG_USER = Action.Id.of("user/kubernetes.kubu_config_user.kubernetes");

    private final KubernetesClusterAgentPoolModule agentPoolModule;

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
        this.agentPoolModule = new KubernetesClusterAgentPoolModule(this);
    }

    public ContainerServiceNetworkProfile getContainerServiceNetworkProfile() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.networkProfile())
                .map(profile -> ContainerServiceNetworkProfile.fromNetworkProfile(profile)).orElse(null);
    }

    @Nullable
    public String getKubernetesVersion() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.version()).orElse(null);
    }

    @Nullable
    public String getApiServerAddress() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.innerModel().fqdn()).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(cluster -> cluster.region()).map(region -> Region.fromName(region.name())).orElse(null);
    }

    @Nullable
    public PowerState getPowerStatus() {
        return Optional.ofNullable(getRemote()).map(remote -> PowerState.fromString(remote.powerState().code().toString())).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(agentPoolModule);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull com.azure.resourcemanager.containerservice.models.KubernetesCluster remote) {
        final String provisioningState = remote.provisioningState();
        return StringUtils.equalsIgnoreCase("Succeeded", provisioningState) ?
                Optional.ofNullable(getPowerStatus()).map(PowerState::getValue).orElse(Status.UNKNOWN) :
                provisioningState;
    }

    @Override
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).start(), AzResource.Status.STARTING);
    }

    @Override
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).stop(), AzResource.Status.STOPPING);
    }

    @Nonnull
    public byte[] getAdminKubeConfig() {
        return Objects.requireNonNull(this.getRemote()).adminKubeConfigContent();
    }

    @Nonnull
    public byte[] getUserKubeConfig() {
        return Objects.requireNonNull(this.getRemote()).userKubeConfigContent();
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
