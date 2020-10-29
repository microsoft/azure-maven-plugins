/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.springcloud;

import org.apache.commons.lang3.BooleanUtils;

public class AppConfig {

    private String subscriptionId;
    private String clusterName;
    private String appName;
    private Boolean isPublic;
    private String resourceGroup;
    private String runtimeVersion;
    private String activeDeploymentName;
    private AppDeploymentConfig deployment;

    public Boolean getIsPublic() {
        return this.isPublic;
    }

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAppName() {
        return appName;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public String getActiveDeploymentName() {
        return activeDeploymentName;
    }

    public AppDeploymentConfig getDeployment() {
        return deployment;
    }

    public AppConfig withPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    public AppConfig withSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    public AppConfig withResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
        return this;
    }

    public AppConfig withClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public AppConfig withAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public AppConfig withRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
        return this;
    }

    public AppConfig withActiveDeploymentName(String deploymentName) {
        this.activeDeploymentName = deploymentName;
        return this;
    }

    public AppConfig withDeployment(AppDeploymentConfig deploymentConfig) {
        this.deployment = deploymentConfig;
        return this;
    }
}
