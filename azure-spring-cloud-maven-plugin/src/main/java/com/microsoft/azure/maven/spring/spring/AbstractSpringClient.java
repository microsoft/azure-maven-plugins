/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;

public abstract class AbstractSpringClient {
    protected String subscriptionId;
    protected String resourceGroup;
    protected String clusterName;
    protected SpringServiceClient springServiceClient;
    protected AppPlatformManager springManager;


    public abstract static class Builder<T extends Builder<T>> {
        protected String subscriptionId;
        protected String clusterName;
        protected SpringServiceClient springServiceClient;

        public T withSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return self();
        }

        public T withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return self();
        }

        public T withSpringServiceClient(SpringServiceClient springServiceClient) {
            this.springServiceClient = springServiceClient;
            return self();
        }

        public abstract AbstractSpringClient build();

        protected abstract T self();
    }

    protected AbstractSpringClient(Builder<?> builder) {
        this.clusterName = builder.clusterName;
        this.subscriptionId = builder.subscriptionId;
        this.springServiceClient = builder.springServiceClient;

        this.springManager = springServiceClient.getSpringManager();
        this.resourceGroup = springServiceClient.getResourceGroupByCluster(clusterName);
    }

    protected AbstractSpringClient(AbstractSpringClient abstractSpringClient) {
        this.clusterName = abstractSpringClient.clusterName;
        this.subscriptionId = abstractSpringClient.subscriptionId;
        this.springManager = abstractSpringClient.springManager;
        this.springServiceClient = abstractSpringClient.springServiceClient;

        this.resourceGroup = springServiceClient.getResourceGroupByCluster(clusterName);
    }
}
