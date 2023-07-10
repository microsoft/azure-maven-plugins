/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
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
    private String subscriptionId;
    private String clusterName;
    private String region;
    private String sku;
    private String environment;
    private String environmentResourceGroup;
    private String appName;
    private String resourceGroup;
    @Builder.Default
    private Boolean isPublic = false;
    @Nullable
    private String activeDeploymentName;
    private SpringCloudDeploymentConfig deployment;

    @Nonnull
    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }

    @Nonnull
    public static SpringCloudAppConfig fromApp(@Nonnull SpringCloudApp app) { // get config from app
        final SpringCloudCluster cluster = app.getParent();
        final SpringCloudDeployment deployment = Optional.ofNullable(app.getActiveDeployment())
            .orElse(app.deployments().getOrDraft("default", app.getResourceGroupName()));
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.fromDeployment(deployment);
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder().deployment(deploymentConfig).build();
        appConfig.setSubscriptionId(app.getSubscriptionId());
        appConfig.setResourceGroup(app.getParent().getResourceGroupName());
        appConfig.setRegion(Objects.requireNonNull(cluster.getRegion()).name());
        appConfig.setClusterName(cluster.getName());
        appConfig.setSku(cluster.getSku().toString()); // todo: ensure convert for sku
        if (cluster.isConsumptionTier()) { // todo: fix the condition for
            final ResourceId environment = ResourceId.fromString(cluster.getManagedEnvironmentId());
            appConfig.setEnvironment(environment.name());
            appConfig.setEnvironmentResourceGroup(environment.resourceGroupName());
        }
        appConfig.setAppName(app.getName());
        appConfig.setIsPublic(Objects.equals(app.isPublicEndpointEnabled(), true));
        return appConfig;
    }
}
