/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.arm.resources.ResourceId;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractAzureManager<T extends HasId> {

    protected boolean isRefreshed = false;

    @Nonnull
    protected String name;
    @Nonnull
    protected String resourceGroup;
    @Nonnull
    protected String subscriptionId;

    protected T remote;

    public AbstractAzureManager(@Nonnull final String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        this.name = resourceId.name();
        this.resourceGroup = resourceId.resourceGroupName();
        this.subscriptionId = resourceId.subscriptionId();
    }

    public AbstractAzureManager(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        this.name = name;
        this.resourceGroup = resourceGroup;
        this.subscriptionId = subscriptionId;
    }

    public AbstractAzureManager(@Nonnull final T resource) {
        this.remote = resource;
        final ResourceId resourceId = ResourceId.fromString(resource.id());
        this.name = resourceId.name();
        this.resourceGroup = resourceId.resourceGroupName();
        this.subscriptionId = resourceId.subscriptionId();
    }

    public final boolean exists() {
        if (Objects.isNull(this.remote) && !this.isRefreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote);
    }

    public synchronized AbstractAzureManager<T> refresh() {
        try {
            this.remote = loadRemote();
        } catch (final ManagementException e) {
            this.remote = null;
            if (HttpStatus.SC_NOT_FOUND != e.getResponse().getStatusCode()) {
                throw e;
            }
        } finally {
            this.isRefreshed = true;
        }
        return this;
    }

    @Nonnull
    protected final T remote() {
        if (Objects.isNull(this.remote) && !isRefreshed) {
            refresh();
        }
        return Objects.requireNonNull(this.remote, "Target resource does not exist.");
    }

    @Nullable
    protected abstract T loadRemote();

}
