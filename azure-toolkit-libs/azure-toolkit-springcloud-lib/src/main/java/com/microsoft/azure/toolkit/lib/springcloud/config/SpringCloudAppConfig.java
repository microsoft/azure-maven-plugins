/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
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

/**
 * @deprecated use {@link com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft} instead.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Deprecated
public class SpringCloudAppConfig {
    private String subscriptionId;
    private String clusterName;
    private String appName;
    private String resourceGroup;
    @Builder.Default
    private Boolean isPublic = false;
    @Builder.Default
    private String runtimeVersion = RuntimeVersion.JAVA_8.toString();
    @Nullable
    private String activeDeploymentName;
    private SpringCloudDeploymentConfig deployment;

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }

    public static SpringCloudAppConfig fromApp(@Nonnull SpringCloudApp app) { // get config from app
        final SpringCloudDeployment deployment = Optional.ofNullable(app.getActiveDeployment())
            .orElse(app.deployments().getOrDraft("default", app.getResourceGroupName()));
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.fromDeployment(deployment);
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder().deployment(deploymentConfig).build();
        appConfig.setSubscriptionId(app.getSubscriptionId());
        appConfig.setResourceGroup(appConfig.getResourceGroup());
        appConfig.setClusterName(app.getParent().getName());
        appConfig.setAppName(app.getName());
        appConfig.setIsPublic(Objects.equals(app.isPublicEndpointEnabled(), true));
        return appConfig;
    }
}
