/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpringCloudCluster implements IAzureEntityManager<SpringCloudClusterEntity> {
    @Getter
    @Nonnull
    final SpringServices client;
    @Nonnull
    private final SpringCloudClusterEntity local;
    @Nullable
    @Getter(AccessLevel.PACKAGE)
    private SpringService remote;

    public SpringCloudCluster(@Nonnull SpringCloudClusterEntity entity, @Nonnull SpringServices client) {
        this.local = entity;
        this.client = client;
        this.remote = entity.getRemote(); // cluster entity must has a SpringService instance when created.
    }

    @Override
    public boolean exists() {
        return Objects.nonNull(this.remote);
    }

    @Nonnull
    public SpringCloudClusterEntity entity() {
        final SpringService remote = Objects.nonNull(this.remote) ? this.remote : this.local.getRemote();
        return new SpringCloudClusterEntity(remote); // prevent inconsistent properties between local and remote when local's properties is modified.
    }

    @Nonnull
    public String id() {
        return this.entity().getId();
    }

    @Nonnull
    public SpringCloudApp app(final String name) {
        if (this.exists() && Objects.nonNull(this.remote)) {
            try {
                final SpringApp app = this.remote.apps().getByName(name);
                return this.app(app);
            } catch (ManagementException ignored) {
            }
        }
        // if app with `name` not exist or this cluster removed?
        return this.app(new SpringCloudAppEntity(name, this.local));
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
    public List<SpringCloudApp> apps() {
        if (this.exists() && Objects.nonNull(this.remote)) {
            return this.remote.apps().list().stream().map(this::app).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Nonnull
    public SpringCloudCluster refresh() {
        final SpringCloudClusterEntity entity = this.entity();
        try {
            this.remote = this.client.getByResourceGroup(entity.getResourceGroup(), entity.getName());
        } catch (ManagementException e) { // if cluster with specified resourceGroup/name removed.
            this.remote = null;
        }
        return this;
    }
}
