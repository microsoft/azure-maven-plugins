/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.service;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;

import java.util.Optional;

public class SpringCloudDeploymentManager {

    private final AppPlatformManager client;

    public SpringCloudDeploymentManager(AppPlatformManager client) {
        this.client = client;
    }

    public void start(String deployment, final SpringCloudAppEntity app) {
        final SpringCloudClusterEntity cluster = app.getCluster();
        this.client.deployments().startAsync(
            cluster.getResourceGroup(),
            cluster.getName(),
            app.getName(),
            deployment
        ).await();
    }

    public void stop(String deployment, final SpringCloudAppEntity app) {
        final SpringCloudClusterEntity cluster = app.getCluster();
        this.client.deployments().stopAsync(
            cluster.getResourceGroup(),
            cluster.getName(),
            app.getName(),
            deployment
        ).await();
    }

    public void restart(String deployment, final SpringCloudAppEntity app) {
        this.client.deployments().restartAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            deployment
        ).await();
    }

    public void remove(String deployment, final SpringCloudAppEntity app) {
        this.client.deployments().deleteAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            deployment).await();
    }

    public SpringCloudDeploymentEntity get(final String deploymentName, final SpringCloudAppEntity app) {
        final DeploymentResourceInner resource = this.client.deployments().inner().get(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            deploymentName
        );
        return Optional.ofNullable(resource).map(r -> SpringCloudDeploymentEntity.fromResource(r, app)).orElse(null);
    }

    public SpringCloudDeploymentEntity create(DeploymentResourceInner inner, final String deploymentName, final SpringCloudAppEntity app) {
        final DeploymentResourceInner resource = this.client.deployments().inner().createOrUpdate(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            deploymentName,
            inner);
        return Optional.ofNullable(resource).map(r -> SpringCloudDeploymentEntity.fromResource(r, app)).orElse(null);
    }

    public SpringCloudDeploymentEntity update(DeploymentResourceInner inner, final SpringCloudDeploymentEntity deployment) {
        final SpringCloudAppEntity app = deployment.getApp();
        final String deploymentName = deployment.getName();
        final DeploymentResourceInner resource = this.client.deployments().inner().update(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            deploymentName,
            inner);
        return Optional.ofNullable(resource).map(r -> SpringCloudDeploymentEntity.fromResource(r, app)).orElse(null);
    }
}
