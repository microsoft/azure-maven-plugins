/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudClusterManager;

import java.util.Objects;

public class SpringCloudCluster implements IAzureEntityManager<SpringCloudClusterEntity> {
    private final AppPlatformManager client;
    private final SpringCloudClusterManager clusterService;
    private final SpringCloudClusterEntity local;
    private SpringCloudClusterEntity remote;
    private boolean refreshed;

    public SpringCloudCluster(SpringCloudClusterEntity cluster, AppPlatformManager client) {
        this.local = cluster;
        this.client = client;
        this.clusterService = new SpringCloudClusterManager(client);
    }

    @Override
    public boolean exists() {
        if (!this.refreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote);
    }

    public SpringCloudClusterEntity entity() {
        return Objects.nonNull(this.remote) ? this.remote : this.local;
    }

    public SpringCloudApp app(SpringCloudAppEntity app) {
        return new SpringCloudApp(app, this);
    }

    public SpringCloudApp app(final String name) {
        final SpringCloudClusterEntity cluster = this.entity();
        return new SpringCloudApp(SpringCloudAppEntity.fromName(name, cluster), this);
    }

    public SpringCloudCluster refresh() {
        final SpringCloudClusterEntity local = this.local;
        this.remote = this.clusterService.get(local.getName(), local.getResourceGroup());
        this.refreshed = true;
        return this;
    }

    public AppPlatformManager getClient() {
        return this.client;
    }
}
