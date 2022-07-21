/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.resourcemanager.containerservice.ContainerServiceManager;
import com.azure.resourcemanager.containerservice.models.ContainerServiceResourceTypes;
import com.azure.resourcemanager.containerservice.models.KubernetesClusters;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class KubernetesClusterModule extends AbstractAzResourceModule<KubernetesCluster, ContainerServiceSubscription,
        com.azure.resourcemanager.containerservice.models.KubernetesCluster> {
    private static final String NAME = "managedClusters";

    public KubernetesClusterModule(@Nonnull final ContainerServiceSubscription parent) {
        super(NAME, parent);
    }

    public List<String> listVirtualMachineVersion(@Nonnull final Region region) {
        return Objects.requireNonNull(getClient()).listOrchestrators(com.azure.core.management.Region.fromName(region.getName()),
                        ContainerServiceResourceTypes.MANAGED_CLUSTERS)
                .stream().map(profile -> profile.orchestratorVersion())
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    protected KubernetesCluster newResource(@Nonnull com.azure.resourcemanager.containerservice.models.KubernetesCluster kubernetesCluster) {
        return new KubernetesCluster(kubernetesCluster, this);
    }

    @Nonnull
    @Override
    protected KubernetesCluster newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new KubernetesCluster(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    public KubernetesClusters getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ContainerServiceManager::kubernetesClusters).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Kubernetes service";
    }

    @Nonnull
    @Override
    protected AzResource.Draft<KubernetesCluster,
            com.azure.resourcemanager.containerservice.models.KubernetesCluster> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new KubernetesClusterDraft(name, Objects.requireNonNull(rgName), this);
    }
}
