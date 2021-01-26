/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudClusterManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureSpringCloud {
    private final AppPlatformManager client;
    private final SpringCloudClusterManager clusterManager;

    private AzureSpringCloud(AppPlatformManager client) {
        this.client = client;
        this.clusterManager = new SpringCloudClusterManager(this.client);
    }

    public static AzureSpringCloud az(AppPlatformManager client) {
        return new AzureSpringCloud(client);
    }

    @Nonnull
    public SpringCloudCluster cluster(@Nonnull SpringCloudClusterEntity cluster) {
        return new SpringCloudCluster(cluster, this.client);
    }

    public SpringCloudCluster cluster(String name, String resourceGroup) {
        final SpringCloudClusterEntity cluster = this.clusterManager.get(name, resourceGroup);
        Objects.requireNonNull(cluster, String.format("cluster(%s) is not found in resource group(%s)", name, resourceGroup));
        return this.cluster(cluster);
    }

    public SpringCloudCluster cluster(String name) {
        final SpringCloudClusterEntity cluster = this.clusterManager.get(name);
        Objects.requireNonNull(cluster, String.format("cluster(%s) is not found", name));
        return this.cluster(cluster);
    }

    public List<SpringCloudCluster> clusters() {
        final List<SpringCloudClusterEntity> clusters = this.clusterManager.getAll();
        return clusters.stream().map(this::cluster).collect(Collectors.toList());
    }
}
