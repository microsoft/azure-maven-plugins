/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import lombok.Data;

import javax.annotation.Nonnull;
import java.util.Optional;

@Data
public class AppRawConfig {
    private String appName;
    private String isPublic;
    private ClusterRawConfig cluster = new ClusterRawConfig();
    private AppDeploymentRawConfig deployment;

    public void saveSpringCloudApp(@Nonnull final SpringCloudApp app) {
        this.cluster = Optional.ofNullable(cluster).orElseGet(ClusterRawConfig::new);
        this.cluster.saveSpringCluster(app.getParent());
        this.setAppName(app.getName());
        this.setIsPublic(String.valueOf(app.isPublicEndpointEnabled()));
        final AppDeploymentRawConfig deploymentSettings = new AppDeploymentRawConfig();
        deploymentSettings.saveSpringCloudDeployment(app.getActiveDeployment());
        this.setDeployment(deploymentSettings);
    }
}
