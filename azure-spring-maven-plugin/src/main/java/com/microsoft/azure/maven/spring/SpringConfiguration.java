/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.maven.spring.configuration.Deployment;

public class SpringConfiguration {

    private boolean isPublic;
    private String subscriptionId;
    private String resourceGroup;
    private String clusterName;
    private String appName;
    private String javaVersion;
    private Deployment deployment;

    public boolean isPublic() {
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

    public String getJavaVersion() {
        return javaVersion;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public SpringConfiguration setPublic(boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    public SpringConfiguration setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    public SpringConfiguration setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
        return this;
    }

    public SpringConfiguration setClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public SpringConfiguration setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public SpringConfiguration setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    public SpringConfiguration setDeployment(Deployment deployment) {
        this.deployment = deployment;
        return this;
    }
}
