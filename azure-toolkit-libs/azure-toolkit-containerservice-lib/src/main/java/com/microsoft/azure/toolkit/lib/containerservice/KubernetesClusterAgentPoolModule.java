/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class KubernetesClusterAgentPoolModule extends
        AbstractAzResourceModule<KubernetesClusterAgentPool, KubernetesCluster, com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool> {

    private static final String NAME = "agentPools";

    public KubernetesClusterAgentPoolModule(@Nonnull KubernetesCluster parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected KubernetesClusterAgentPool newResource(
            @Nonnull com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool kubernetesClusterAgentPool) {
        return new KubernetesClusterAgentPool(kubernetesClusterAgentPool, this);
    }

    @Nonnull
    @Override
    protected KubernetesClusterAgentPool newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        throw new UnsupportedOperationException("not support");
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Node pool";
    }

    @Nonnull
    @Override
    protected Stream<com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(cluster -> cluster.agentPools().values().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool loadResourceFromAzure(
            @Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(this.getClient()).map(cluster -> cluster.agentPools().get(name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "kubernetes.delete_cluster.cluster", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.SERVICE)
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final String name = ResourceId.fromString(resourceId).name();
        Optional.ofNullable(getClient()).ifPresent(cluster -> cluster.update().withoutAgentPool(name));
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.containerservice.models.KubernetesCluster getClient() {
        return this.parent.getRemote();
    }
}
