/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.Microservices4SpringManager;

public abstract class AbstractSpringClient {
    protected String subscriptionId;
    protected String resourceGroup;
    protected String clusterName;
    protected Microservices4SpringManager springManager;


    public abstract static class Builder<T extends Builder<T>> {
        protected String subscriptionId;
        protected String clusterName;

        public T withSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return self();
        }

        public T withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return self();
        }

        public abstract AbstractSpringClient build();

        protected abstract T self();
    }

    protected AbstractSpringClient(Builder<?> builder) {
        this.clusterName = builder.clusterName;
        this.subscriptionId = builder.subscriptionId;
        this.springManager = getSpringManager();

        this.resourceGroup = SpringServiceUtils.getResourceGroupByCluster(clusterName, subscriptionId);
    }

    protected AbstractSpringClient(AbstractSpringClient abstractSpringClient) {
        this.clusterName = abstractSpringClient.clusterName;
        this.subscriptionId = abstractSpringClient.subscriptionId;
        this.springManager = abstractSpringClient.springManager;

        this.resourceGroup = SpringServiceUtils.getResourceGroupByCluster(clusterName, subscriptionId);
    }

    protected Microservices4SpringManager getSpringManager() {
        return SpringServiceUtils.getSpringManager(subscriptionId);
    }
}
