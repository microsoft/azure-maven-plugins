/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.service;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentEntity;

public class SpringCloudDeploymentService {

    private final AppPlatformManager client;

    public SpringCloudDeploymentService(AppPlatformManager client) {
        this.client = client;
    }

    public void start(final SpringCloudDeploymentEntity deployment) {
        this.start(deployment.getName(), deployment.getApp());
    }

    public void stop(final SpringCloudDeploymentEntity deployment) {
        this.stop(deployment.getName(), deployment.getApp());
    }

    public void restart(final SpringCloudDeploymentEntity deployment) {
        this.restart(deployment.getName(), deployment.getApp());
    }

    public void remove(final SpringCloudDeploymentEntity deployment) {
        this.remove(deployment.getName(), deployment.getApp());
    }

    public void start(String deployment, final SpringCloudAppEntity app) {
        this.client.deployments().startAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            deployment
        ).await();
    }

    public void stop(String deployment, final SpringCloudAppEntity app) {
        this.client.deployments().stopAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
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

    public SpringCloudDeploymentEntity reload(final SpringCloudDeploymentEntity deployment) {
        final DeploymentResourceInner resource = this.client.deployments().inner().get(
            deployment.getApp().getCluster().getResourceGroup(),
            deployment.getApp().getCluster().getName(),
            deployment.getApp().getName(),
            deployment.getName()
        );
        return SpringCloudDeploymentEntity.fromResource(resource);
    }

    public SpringCloudDeploymentEntity create(DeploymentResourceInner resource, final SpringCloudDeploymentEntity deployment) {
        final DeploymentResourceInner result = this.client.deployments().inner().createOrUpdate(
            deployment.getApp().getCluster().getResourceGroup(),
            deployment.getApp().getCluster().getName(),
            deployment.getApp().getName(),
            deployment.getName(),
            resource);
        return SpringCloudDeploymentEntity.fromResource(result);
    }

    public SpringCloudDeploymentEntity update(DeploymentResourceInner resource, final SpringCloudDeploymentEntity deployment) {
        final DeploymentResourceInner result = this.client.deployments().inner().update(
            deployment.getApp().getCluster().getResourceGroup(),
            deployment.getApp().getCluster().getName(),
            deployment.getApp().getName(),
            deployment.getName(),
            resource);
        return SpringCloudDeploymentEntity.fromResource(result);
    }
}
