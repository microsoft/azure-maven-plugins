/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.service;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.appplatform.v2020_07_01.ServiceResource;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudClusterEntity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpringCloudClusterService {

    private final AppPlatformManager client;

    public SpringCloudClusterService(AppPlatformManager client) {
        this.client = client;
    }

    public void remove(final SpringCloudClusterEntity service) {
        this.client.services().deleteAsync(
            service.getResourceGroup(),
            service.getName()).await();
    }

    public SpringCloudClusterEntity get(final String clusterName) {
        final List<SpringCloudClusterEntity> clusters = this.getAll();
        return clusters.stream().filter((s) -> Objects.equals(s.getName(), clusterName)).findAny().orElse(null);
    }

    public SpringCloudClusterEntity get(final String clusterName, final String resourceGroup) {
        final ServiceResource s = this.client.services().getByResourceGroupAsync(
            resourceGroup,
            clusterName).toBlocking().first();
        return Optional.ofNullable(s).map(r -> SpringCloudClusterEntity.fromResource(r.inner())).orElse(null);
    }

    public List<SpringCloudAppEntity> getApps(final SpringCloudClusterEntity cluster) {
        final PagedList<AppResourceInner> apps = this.client.apps().inner().list(
            cluster.getResourceGroup(),
            cluster.getName());
        apps.loadAll();
        return apps.stream().map(app -> SpringCloudAppEntity.fromResource(app, cluster)).collect(Collectors.toList());
    }

    public List<SpringCloudClusterEntity> getAll() {
        final PagedList<ServiceResourceInner> clusters = this.client.inner().services().list();
        clusters.loadAll();
        return clusters.stream().map(SpringCloudClusterEntity::fromResource).collect(Collectors.toList());
    }

    public String getSubscriptionId() {
        return this.client.subscriptionId();
    }
}
