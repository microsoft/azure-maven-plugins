/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractAzureManager<T extends HasId> {

    protected boolean isRefreshed = false;

    @Nonnull
    protected final String name;
    @Nonnull
    protected final String resourceGroup;
    @Nonnull
    protected final String subscriptionId;

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
        this(resource.id());
        this.remote = resource;
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
        if (!exists()) {
            throw new AzureToolkitRuntimeException(String.format("Target resource %s does not exist.", name));
        }
        return this.remote;
    }

    @Nullable
    protected abstract T loadRemote();

}
