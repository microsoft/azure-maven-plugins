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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public SpringCloudClusterEntity reload(final SpringCloudClusterEntity service) {
        final ServiceResource s = this.client.services().getByResourceGroupAsync(
            service.getResourceGroup(),
            service.getName()).toBlocking().first();
        return SpringCloudClusterEntity.from(s.inner());
    }

    public List<SpringCloudAppEntity> getApps(final SpringCloudClusterEntity service) {
        final PagedList<AppResourceInner> page = this.client.apps().inner().list(
            service.getResourceGroup(),
            service.getName());
        page.loadAll();
        return page.stream().map(SpringCloudAppEntity::fromResource).collect(Collectors.toList());
    }

    public List<SpringCloudClusterEntity> getAllClusters() {
        final PagedList<ServiceResourceInner> clusterList = this.client.inner().services().list();
        clusterList.loadAll();
        return new ArrayList<>(clusterList).stream().map(SpringCloudClusterEntity::from).collect(Collectors.toList());
    }

    @Nullable
    public SpringCloudClusterEntity getCluster(String name) {
        final List<SpringCloudClusterEntity> services = this.getAllClusters();
        return services.stream().filter((s) -> Objects.equals(s.getName(), name)).findFirst().orElse(null);
    }

    public String getSubscriptionId() {
        return this.client.subscriptionId();
    }
}
