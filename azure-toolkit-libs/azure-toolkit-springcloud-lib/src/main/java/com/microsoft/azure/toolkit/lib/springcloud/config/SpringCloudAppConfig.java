/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SpringCloudAppConfig {
    private SpringCloudClusterConfig cluster;
    private String appName;
    @Builder.Default
    private Boolean isPublic = false;
    @Nullable
    private String activeDeploymentName;
    private SpringCloudDeploymentConfig deployment;

    @Nonnull
    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }

    public String getSubscriptionId() {
        return Optional.ofNullable(cluster).map(SpringCloudClusterConfig::getSubscriptionId).orElse(null);
    }

    public void setSubscriptionId(String subscriptionId) {
        this.ensureCluster().setSubscriptionId(subscriptionId);
    }

    public String getResourceGroup() {
        return Optional.ofNullable(cluster).map(SpringCloudClusterConfig::getResourceGroup).orElse(null);
    }

    public void setResourceGroup(String resourceGroup) {
        this.ensureCluster().setResourceGroup(resourceGroup);
    }

    public String getClusterName() {
        return Optional.ofNullable(cluster).map(SpringCloudClusterConfig::getClusterName).orElse(null);
    }

    public void setClusterName(String clusterName) {
        this.ensureCluster().setClusterName(clusterName);
    }

    private SpringCloudClusterConfig ensureCluster() {
        this.cluster = Optional.ofNullable(cluster).orElseGet(SpringCloudClusterConfig::new);
        return this.cluster;
    }

    @Nonnull
    public static SpringCloudAppConfig fromApp(@Nonnull SpringCloudApp app) { // get config from app
        final SpringCloudDeployment deployment = Optional.ofNullable(app.getActiveDeployment())
            .orElse(app.deployments().getOrDraft("default", app.getResourceGroupName()));
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.fromDeployment(deployment);
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder().deployment(deploymentConfig).build();
        appConfig.setCluster(SpringCloudClusterConfig.fromCluster(app.getParent()));
        appConfig.setAppName(app.getName());
        appConfig.setIsPublic(Objects.equals(app.isPublicEndpointEnabled(), true));
        return appConfig;
    }
}
