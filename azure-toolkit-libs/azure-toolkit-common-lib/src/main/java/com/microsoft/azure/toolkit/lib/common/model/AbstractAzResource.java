/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
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
    @Getter(AccessLevel.NONE)
    private long syncTime;
    @Getter(AccessLevel.NONE)
    private String status = Status.DISCONNECTED;
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
                final R remote = this.module.loadResourceFromAzure(this.module.toResourceId(this.name, this.resourceGroup));
                this.setRemote(remote);
            } catch (Throwable t) { // TODO: handle exception
                this.setRemote(null);
            }
        });
        this.doModifyAsync(() -> {
            this.getSubModules().parallelStream().forEach(AzResourceModule::refresh);
            AzureEventBus.emit("resource.refresh_children.resource", this);
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
                this.setRemote(remote);
                this.module.addResourceToLocal((T) this);
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

    protected void setRemote(R remote) {
        final R oldRemote = this.remote;
        if (!Objects.equals(oldRemote, remote)) {
            this.remote = remote;
            this.syncTime = Objects.nonNull(remote) ? System.currentTimeMillis() : -1;
            if (Objects.nonNull(remote)) {
                this.doModifyAsync(() -> this.setStatus(this.loadStatus(remote)));
            } else {
                this.setStatus(Status.DISCONNECTED);
            }
        }
    }

    @Override
    @Nullable
    public final R getRemote() {
        if (this.syncTime < 0) {
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
    public String getStatus() {
        final R remote = this.getRemote();
        if (Objects.nonNull(remote) && Objects.isNull(this.status)) {
            this.setStatus(this.loadStatus(remote));
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

    public void doModifyAsync(Runnable body) {
        // TODO: lock so that can not modify if modifying.
        this.setStatus(Status.PENDING);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                this.setStatus(Status.ERROR);
                throw t;
            }
        });
    }

    @Nonnull
    public String getId() {
        return this.module.toResourceId(this.name, this.resourceGroup);
    }

    public abstract List<AzResourceModule<?, T>> getSubModules();

    public String formalizeStatus(String status) {
        return status;
    }

    @Nonnull
    public abstract String loadStatus(@Nonnull R remote);
}
