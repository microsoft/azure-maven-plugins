/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.service;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appplatform.v2020_07_01.AppResource;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudAppEntity;
import com.microsoft.azure.tools.utils.StorageUtils;

import javax.annotation.Nonnull;
import java.io.File;

public class SpringCloudAppService {
    private final AppPlatformManager client;
    private final SpringCloudDeploymentService deploymentService;

    public SpringCloudAppService(AppPlatformManager client) {
        this.client = client;
        this.deploymentService = new SpringCloudDeploymentService(this.client);
    }

    public void start(final SpringCloudAppEntity app) {
        this.deploymentService.start(app.inner().properties().activeDeploymentName(), app);
    }

    public void stop(final SpringCloudAppEntity app) {
        this.deploymentService.stop(app.inner().properties().activeDeploymentName(), app);
    }

    public void restart(final SpringCloudAppEntity app) {
        this.deploymentService.restart(app.inner().properties().activeDeploymentName(), app);
    }

    public void remove(final SpringCloudAppEntity app) {
        this.client.apps().deleteAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName()
        ).await();
    }

    public SpringCloudAppEntity get(final SpringCloudAppEntity app) {
        final AppResource resource = this.client.apps().getAsync(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName()
        ).toBlocking().first();
        return SpringCloudAppEntity.fromResource(resource.inner());
    }

    @Nonnull
    public SpringCloudAppEntity update(AppResourceInner resource, final SpringCloudAppEntity app) {
        final AppResourceInner result = this.client.apps().inner().update(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            resource
        );
        return SpringCloudAppEntity.fromResource(result);
    }

    @Nonnull
    public SpringCloudAppEntity create(AppResourceInner resource, final SpringCloudAppEntity app) {
        final AppResourceInner result = this.client.apps().inner().createOrUpdate(
            app.getCluster().getResourceGroup(),
            app.getCluster().getName(),
            app.getName(),
            resource
        );
        return SpringCloudAppEntity.fromResource(result);
    }

    public ResourceUploadDefinitionInner uploadArtifact(String path, final SpringCloudAppEntity app) throws AzureExecutionException {
        final ResourceUploadDefinitionInner definition = this.getUploadDefinition(app);
        StorageUtils.uploadFileToStorage(new File(path), definition.uploadUrl());
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
