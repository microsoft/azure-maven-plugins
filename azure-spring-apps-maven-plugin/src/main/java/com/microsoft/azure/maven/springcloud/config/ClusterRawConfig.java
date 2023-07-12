/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import lombok.Data;

import javax.annotation.Nonnull;

@Data
public class ClusterRawConfig {
    private String subscriptionId;
    private String resourceGroup; // optional
    private String clusterName;
    private String region;
    private String sku;
    private String environment;
    private String environmentResourceGroup;

    public void saveSpringCluster(@Nonnull final SpringCloudCluster cluster) {
        this.setSubscriptionId(cluster.getSubscriptionId());
        this.setResourceGroup(cluster.getResourceGroupName());
        this.setClusterName(cluster.getName());
    }
}
