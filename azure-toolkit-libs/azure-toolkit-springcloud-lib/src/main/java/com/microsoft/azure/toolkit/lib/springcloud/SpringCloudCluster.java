/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.common.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudClusterEntity;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudClusterService;

import java.util.Objects;

public class SpringCloudCluster implements IAzureEntityManager<SpringCloudClusterEntity> {
    private final AppPlatformManager client;
    private final SpringCloudClusterService service;
    private final SpringCloudClusterEntity local;
    private SpringCloudClusterEntity remote;

    public SpringCloudCluster(SpringCloudClusterEntity cluster, AppPlatformManager client) {
        this.local = cluster;
        this.client = client;
        this.service = new SpringCloudClusterService(client);
    }

    @Override
    public boolean exists() {
        this.reload();
        return Objects.nonNull(this.remote);
    }

    public SpringCloudClusterEntity entity() {
        return Objects.nonNull(this.remote) ? this.remote : this.local;
    }

    public SpringCloudApp app(SpringCloudAppEntity app) {
        return new SpringCloudApp(app, this);
    }

    public SpringCloudCluster reload() {
        this.remote = this.service.reload(this.local);
        return this;
    }

    public AppPlatformManager getClient() {
        return this.client;
    }
}
