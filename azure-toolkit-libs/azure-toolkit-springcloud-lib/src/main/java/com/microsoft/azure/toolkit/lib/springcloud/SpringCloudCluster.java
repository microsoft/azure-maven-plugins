/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.models.ProvisioningState;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import lombok.Getter;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpringCloudCluster
        extends AbstractAzureResource<SpringCloudCluster, SpringCloudClusterEntity, SpringService>
        implements AzureOperationEvent.Source<SpringCloudCluster> {
    @Getter
    @Nonnull
    final SpringServices client;

    public SpringCloudCluster(@Nonnull SpringCloudClusterEntity entity, @Nonnull SpringServices client) {
        super(entity);
        this.client = client;
    }

    @Override
    @AzureOperation(name = "springcloud.load_cluster.cluster", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    protected SpringService loadRemote() {
        try {
            return this.client.getByResourceGroup(entity.getResourceGroup(), entity.getName());
        } catch (ManagementException e) { // if cluster with specified resourceGroup/name removed.
            if (HttpStatus.SC_NOT_FOUND == e.getResponse().getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    @Nonnull
    @Cacheable(cacheName = "resource/{}/child/{}", key = "${this.id()}/$name")
    @AzureOperation(name = "springcloud.get_app.app", params = {"name"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudApp app(final String name) {
        if (this.exists()) {
            try {
                final SpringApp app = Objects.requireNonNull(this.remote()).apps().getByName(name);
                return this.app(app);
            } catch (ManagementException ignored) {
            }
        }
        // if app with `name` not exist or this cluster removed?
        return this.app(new SpringCloudAppEntity(name, this.entity));
    }

    @Nonnull
    SpringCloudApp app(@Nonnull SpringApp app) {
        return this.app(new SpringCloudAppEntity(app, this.entity()));
    }

    @Nonnull
    public SpringCloudApp app(@Nonnull SpringCloudAppEntity app) {
        return new SpringCloudApp(app, this);
    }

    @Nonnull
    public SpringCloudApp app(@Nonnull SpringCloudAppConfig config) {
        return this.app(new SpringCloudAppEntity(config, this.entity));
    }

    @Nonnull
    @Cacheable(cacheName = "resource/{}/children", key = "${this.id()}")
    @AzureOperation(name = "springcloud.list_apps.cluster", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public List<SpringCloudApp> apps() {
        if (this.exists()) {
            return Objects.requireNonNull(this.remote()).apps().list().stream().map(this::app).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    protected String loadStatus() {
        final SpringService remote = this.remote();
        if (Objects.isNull(remote)) {
            return Status.UNKNOWN;
        }
        final ProvisioningState state = remote.refresh().innerModel().properties().provisioningState();
        switch (state.toString().toUpperCase()) {
            case "CREATING":
            case "MOVING":
            case "UPDATING":
            case "DELETING":
                return Status.PENDING;
            case "SUCCEEDED":
                return Status.RUNNING;
            case "MOVEFAILED":
            case "DELETED":
            case "MOVED":
            case "FAILED":
                return Status.ERROR;
            default:
                return Status.UNKNOWN;
        }
    }
}
