/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractAzureResource<T extends HasId, P extends IAzureBaseResource> implements IAzureBaseResource<AbstractAzureResource<T, P>, P> {
    @Nonnull
    @Getter
    protected final String name;
    @Getter
    @Nonnull
    protected final String resourceGroup;
    @Getter
    @Nonnull
    protected final String subscriptionId;
    @Getter
    protected String id;

    protected T remote;
    protected String status = null;
    protected boolean isRefreshed = false;

    public AbstractAzureResource(@Nonnull final String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        this.id = id;
        this.name = resourceId.name();
        this.resourceGroup = resourceId.resourceGroupName();
        this.subscriptionId = resourceId.subscriptionId();
    }

    public AbstractAzureResource(@Nonnull final T resource) {
        this(resource.id());
        this.remote = resource;
        this.id = resource.id();
    }

    @Override
    public AbstractAzureResource<T, P> refresh() {
        try {
            this.remote = loadRemote();
            this.isRefreshed = true;
        } catch (final ManagementException e) {
            if (HttpStatus.SC_NOT_FOUND == e.getResponse().getStatusCode()) {
                this.remote = null;
                this.isRefreshed = true;
            } else {
                throw e;
            }
        }
        return this;
    }

    @Override
    public boolean exists() {
        if (Objects.isNull(this.remote) && !this.isRefreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String id() {
        return getId();
    }

    @Nonnull
    protected final T remote() {
        if (!exists()) {
            throw new AzureToolkitRuntimeException(String.format("Target resource %s does not exist.", name));
        }
        return this.remote;
    }

    @Override
    public final String status() {
        if (Objects.nonNull(this.status)) {
            return this.status;
        } else {
            this.refreshStatus();
            return Status.LOADING;
        }
    }

    public final void refreshStatus() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.status(this.loadStatus()));
    }

    protected final void status(@Nonnull String status) {
        final String oldStatus = this.status;
        this.status = status;
        if (!StringUtils.equalsIgnoreCase(oldStatus, this.status)) {
            AzureEventBus.emit("common|resource.status_changed", this);
        }
    }

    @Nullable
    protected abstract T loadRemote();

    protected String loadStatus() {
        return Status.RUNNING;
    }
}
