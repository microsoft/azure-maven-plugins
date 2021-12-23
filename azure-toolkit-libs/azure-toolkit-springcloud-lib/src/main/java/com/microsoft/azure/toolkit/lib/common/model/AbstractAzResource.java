/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Getter
public abstract class AbstractAzResource<T extends AbstractAzResource<T, P, R>, P extends AzResource<P, ?, ?>, R> implements AzResource<T, P, R> {
    @Nonnull
    private final String name;
    @Nonnull
    private final String resourceGroup;
    @Nonnull
    private final AbstractAzResourceModule<T, P, R> module;
    @Nullable
    @Getter(AccessLevel.NONE)
    private R remote;
    private boolean refreshed;
    @Getter(AccessLevel.NONE)
    private String status;
    @Setter
    private Object config;

    protected AbstractAzResource(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        this.name = name;
        this.resourceGroup = resourceGroup;
        this.module = module;
    }

    public final boolean exists() {
        return Objects.nonNull(this.getRemote());
    }

    @Override
    public void refresh() {
        this.doModify(() -> {
            try {
                this.setRemote(this.module.loadResourceFromAzure(this.module.toResourceId(this.name, this.resourceGroup)));
            } catch (Throwable t) { // TODO: handle exception
                this.setRemote(null);
            }
        });
    }

    public void create() { // TODO: add async one
        this.create(null);
    }

    @Override
    public void create(Object config) { // TODO: add async one
        try {
            this.doModify(() -> {
                final R remote = this.module.createResourceInAzure(name, resourceGroup, config);
                this.module.addResourceToLocal((T) this);
                this.setRemote(remote);
            });
        } catch (Throwable e) { // TODO: handle exception
        }
    }

    @Override
    public void update(@Nonnull Object config) { // TODO: add async one
        try {
            assert this.exists();
            this.doModify(() -> {
                final R remote = this.module.updateResourceInAzure(Objects.requireNonNull(this.getRemote()), config);
                this.setRemote(remote);
            });
        } catch (Throwable e) { // TODO: handle exception
        }
    }

    @Override
    public void delete() { // TODO: add async one
        try {
            assert this.exists();
            this.doModify(() -> {
                this.module.deleteResourceFromAzure(this.getId());
                this.module.deleteResourceFromLocal(name);
            });
        } catch (Throwable ignored) { // TODO: handle exception
        }
    }

    public final void setRemote(R remote) {
        this.remote = remote;
        this.setStatus(Objects.isNull(remote) ? Status.DISCONNECTED : this.loadStatus(this.remote));
        this.refreshed = Objects.nonNull(remote);
    }

    @Override
    @Nullable
    public final R getRemote() {
        if (!this.refreshed) {
            this.refresh();
        }
        return this.remote;
    }

    private synchronized void setStatus(@Nonnull String status) {
        final String oldStatus = this.status;
        if (!Objects.equals(oldStatus, status)) {
            this.status = status;
            AzureEventBus.emit("common|resource.status_changed", this);
        }
    }

    @Nonnull
    public String getId() {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("%s/%s", this.getModule().getId(), name).replace(AzResource.RESOURCE_GROUP_PLACEHOLDER, rg);
    }

    @Nonnull
    public String getStatus() {
        if (!this.refreshed) {
            this.refresh();
        }
        return this.status;
    }

    public void doModify(Runnable body) {
        // TODO: lock so that can not modify if modifying.
        this.setStatus(Status.PENDING);
        try {
            body.run();
        } catch (Throwable t) {
            this.setStatus(Status.ERROR);
            throw t;
        }
    }

    public String formalizeStatus(String status) {
        return status;
    }

    @Nonnull
    public abstract String loadStatus(@Nonnull R remote);
}
