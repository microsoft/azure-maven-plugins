/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;

public abstract class AbstractClientBase {
    protected final ServiceResourceInner cluster;
    protected String subscriptionId;
    protected String resourceGroup;
    protected String clusterName;
    protected ServiceClient springServiceClient;
    protected AppPlatformManager springManager;

    public abstract static class Builder<T extends Builder<T>> {
        protected String subscriptionId;
        protected String clusterName;
        protected ServiceClient springServiceClient;

        public T withSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return self();
        }

        public T withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return self();
        }

        public T withSpringServiceClient(ServiceClient springServiceClient) {
            this.springServiceClient = springServiceClient;
            return self();
        }

        public abstract AbstractClientBase build();

        protected abstract T self();
    }

    protected AbstractClientBase(Builder<?> builder) {
        this.clusterName = builder.clusterName;
        this.subscriptionId = builder.subscriptionId;
        this.springServiceClient = builder.springServiceClient;

        this.springManager = springServiceClient.getSpringManager();
        this.cluster = springServiceClient.getClusterByName(clusterName);
        this.resourceGroup = springServiceClient.getResourceGroupByCluster(this.cluster);
    }

    protected AbstractClientBase(AbstractClientBase cloudAppClient) {
        this.clusterName = cloudAppClient.clusterName;
        this.subscriptionId = cloudAppClient.subscriptionId;
        this.springManager = cloudAppClient.springManager;
        this.springServiceClient = cloudAppClient.springServiceClient;
        this.cluster = springServiceClient.getClusterByName(clusterName);
        this.resourceGroup = springServiceClient.getResourceGroupByCluster(this.cluster);
    }
}
