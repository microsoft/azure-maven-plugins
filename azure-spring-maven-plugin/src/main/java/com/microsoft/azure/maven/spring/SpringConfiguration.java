/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.maven.spring.configuration.Deployment;

public class SpringConfiguration {

    private Boolean isPublic;
    private String subscriptionId;
    private String resourceGroup;
    private String clusterName;
    private String appName;
    private String runtimeVersion;
    private Deployment deployment;

    public Boolean isPublic() {
        return isPublic;
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

    public Deployment getDeployment() {
        return deployment;
    }

    public SpringConfiguration withPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    public SpringConfiguration withSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    public SpringConfiguration withResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
        return this;
    }

    public SpringConfiguration withClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public SpringConfiguration withAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public SpringConfiguration withRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
        return this;
    }

    public SpringConfiguration withDeployment(Deployment deployment) {
        this.deployment = deployment;
        return this;
    }
}
