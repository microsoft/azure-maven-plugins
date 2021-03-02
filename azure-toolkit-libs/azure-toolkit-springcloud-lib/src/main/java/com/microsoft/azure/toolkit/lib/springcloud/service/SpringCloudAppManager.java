/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.service;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.Utils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpringCloudAppManager {
    private final AppPlatformManager client;

    public SpringCloudAppManager(AppPlatformManager client) {
        this.client = client;
    }

    public void remove(final SpringCloudAppEntity app) {
        this.client.apps().deleteAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName()
        ).await();
    }

    public SpringCloudAppEntity get(final String appName, final SpringCloudClusterEntity cluster) {
        final AppResourceInner resource = this.client.apps().inner().get(
            cluster.getResourceGroup(),
            cluster.getName(),
            appName
        );
        return Optional.ofNullable(resource).map(r -> SpringCloudAppEntity.fromResource(r, cluster)).orElse(null);
    }

    public SpringCloudAppEntity create(AppResourceInner inner, final String appName, final SpringCloudClusterEntity cluster) {
        final AppResourceInner resource = this.client.apps().inner().createOrUpdate(
            cluster.getResourceGroup(),
            cluster.getName(),
            appName,
            inner
        );
        return Optional.ofNullable(resource).map(r -> SpringCloudAppEntity.fromResource(r, cluster)).orElse(null);
    }

    public SpringCloudAppEntity update(AppResourceInner inner, final SpringCloudAppEntity app) {
        final SpringCloudClusterEntity cluster = app.getCluster();
        final AppResourceInner resource = this.client.apps().inner().update(
            cluster.getResourceGroup(),
            cluster.getName(),
            app.getName(),
            inner
        );
        return Optional.ofNullable(resource).map(r -> SpringCloudAppEntity.fromResource(r, cluster)).orElse(null);
    }

    public List<SpringCloudDeploymentEntity> getDeployments(final SpringCloudAppEntity app) {
        final SpringCloudClusterEntity cluster = app.getCluster();
        final PagedList<DeploymentResourceInner> deployments = this.client.inner().deployments().list(
            cluster.getResourceGroup(),
            cluster.getName(),
            app.getName());
        deployments.loadAll();
        return deployments.stream().map(d -> SpringCloudDeploymentEntity.fromResource(d, app)).collect(Collectors.toList());
    }

    public ResourceUploadDefinitionInner uploadArtifact(String path, final SpringCloudAppEntity app) {
        final ResourceUploadDefinitionInner definition = this.getUploadDefinition(app);
        Utils.uploadFileToStorage(new File(path), definition.uploadUrl());
        return definition;
    }

    public ResourceUploadDefinitionInner getUploadDefinition(final SpringCloudAppEntity app) {
        return this.client.apps().inner().getResourceUploadUrlAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName()
        ).toBlocking().first();
    }
}
