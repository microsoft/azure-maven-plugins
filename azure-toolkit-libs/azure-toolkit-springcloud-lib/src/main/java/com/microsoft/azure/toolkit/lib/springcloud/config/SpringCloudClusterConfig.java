/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SpringCloudClusterConfig {
    private String subscriptionId;
    private String resourceGroup;
    private String clusterName;
    private String region;
    private String sku;
    private String environment;
    private String environmentResourceGroup;

    @Nonnull
    public static SpringCloudClusterConfig fromCluster(@Nonnull final SpringCloudCluster cluster) {
        final SpringCloudClusterConfig config = new SpringCloudClusterConfig();
        config.setSubscriptionId(cluster.getSubscriptionId());
        config.setResourceGroup(cluster.getParent().getResourceGroupName());
        config.setRegion(Objects.requireNonNull(cluster.getRegion()).name());
        config.setClusterName(cluster.getName());
        config.setSku(cluster.getSku().toString()); // todo: ensure convert for sku
        if (cluster.isConsumptionTier()) { // todo: fix the condition for
            final ResourceId environment = ResourceId.fromString(cluster.getManagedEnvironmentId());
            config.setEnvironment(environment.name());
            config.setEnvironmentResourceGroup(environment.resourceGroupName());
        }
        return config;
    }
}
