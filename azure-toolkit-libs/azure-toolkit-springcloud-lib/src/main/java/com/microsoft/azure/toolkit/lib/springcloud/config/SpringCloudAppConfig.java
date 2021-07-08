/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SpringCloudAppConfig {
    private String subscriptionId;
    private String clusterName;
    private String appName;
    private String resourceGroup;
    private Boolean isPublic;
    private String runtimeVersion;
    private String activeDeploymentName;
    private SpringCloudDeploymentConfig deployment;

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }

    public static SpringCloudAppConfig fromApp(@Nonnull SpringCloudApp app) { // get config from app
        final SpringCloudAppEntity appEntity = app.entity();
        return fromApp(appEntity);
    }

    @Nonnull
    public static SpringCloudAppConfig fromApp(SpringCloudAppEntity appEntity) {
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.fromDeployment(appEntity.activeDeployment());
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder().deployment(deploymentConfig).build();
        appConfig.setSubscriptionId(appEntity.getSubscriptionId());
        appConfig.setClusterName(appEntity.getCluster().getName());
        appConfig.setAppName(appEntity.getName());
        appConfig.setIsPublic(Objects.equals(appEntity.isPublic(), true));
        return appConfig;
    }
}
