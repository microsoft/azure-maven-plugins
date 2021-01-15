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
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudClusterEntity;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudClusterService;

import java.util.List;
import java.util.stream.Collectors;

public class AzureSpringCloud {
    private final AppPlatformManager client;

    private AzureSpringCloud(AppPlatformManager client) {
        this.client = client;
    }

    public static AzureSpringCloud az(AppPlatformManager client) {
        return new AzureSpringCloud(client);
    }

    public SpringCloudCluster cluster(SpringCloudClusterEntity cluster) {
        return new SpringCloudCluster(cluster, this.client);
    }

    public SpringCloudCluster cluster(String name) {
        final SpringCloudClusterService service = new SpringCloudClusterService(this.client);
        return this.cluster(service.getCluster(name));
    }

    public List<SpringCloudCluster> clusters() {
        final SpringCloudClusterService service = new SpringCloudClusterService(this.client);
        return service.getAllClusters().stream().map(this::cluster).collect(Collectors.toList());
    }

}
