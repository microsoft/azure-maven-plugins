/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import lombok.Data;

import javax.annotation.Nonnull;

@Data
public class AppRawConfig {
    private String subscriptionId;
    private String resourceGroup; // optional
    private String clusterName;
    private String appName;
    private String isPublic;
    private String region;
    private String sku;
    private String environment;
    private String environmentResourceGroup;
    private AppDeploymentRawConfig deployment;

    public void saveSpringCloudApp(@Nonnull final SpringCloudApp app) {
        this.setSubscriptionId(app.getSubscriptionId());
        this.setResourceGroup(app.getResourceGroupName());
        this.setClusterName(app.getParent().getName());
        this.setAppName(app.getName());
        this.setIsPublic(String.valueOf(app.isPublicEndpointEnabled()));
        final AppDeploymentRawConfig deploymentSettings = new AppDeploymentRawConfig();
        deploymentSettings.saveSpringCloudDeployment(app.getActiveDeployment());
        this.setDeployment(deploymentSettings);
    }
}
