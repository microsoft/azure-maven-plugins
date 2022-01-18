/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.model.Refreshable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractAzResource<T extends AbstractAzResource<T, P, R>, P extends AbstractAzResource<P, ?, ?>, R> implements AzResource<T, P, R> {
    @Nonnull
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String name;
    @Nonnull
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String resourceGroupName;
    @Nonnull
    @Getter
    @EqualsAndHashCode.Include
    private final AbstractAzResourceModule<T, P, R> module;
    @Nonnull
    final AtomicReference<R> remoteRef;
    @ToString.Include
    final AtomicLong syncTimeRef;
    @ToString.Include
    final AtomicReference<String> statusRef;

    protected AbstractAzResource(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        this.name = name;
        this.resourceGroupName = resourceGroupName;
        this.module = module;
        this.remoteRef = new AtomicReference<>();
        this.syncTimeRef = new AtomicLong(-1);
        this.statusRef = new AtomicReference<>("");
    }

    /**
     * constructor for non-top resource only.
     * {@link AbstractAzResource#getResourceGroupName() module.getParent().getResourceGroupName()} is only reliable
     * if current resource is not root of resource hierarchy tree.
     */
    protected AbstractAzResource(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        this(name, module.getParent().getResourceGroupName(), module);
    }

    /**
     * copy constructor
     */
    protected AbstractAzResource(@Nonnull T origin) {
        this.name = origin.getName();
        this.resourceGroupName = origin.getResourceGroupName();
        this.module = origin.getModule();
        this.remoteRef = origin.remoteRef;
        this.statusRef = origin.statusRef;
        this.syncTimeRef = origin.syncTimeRef;
    }

    public final boolean exists() {
        return Objects.nonNull(this.getRemote());
    }

    @Override
    public void refresh() {
        this.syncTimeRef.set(-1);
        AzureEventBus.emit("resource.status_changed.resource", this);
        this.getSubModules().forEach(AzResourceModule::refresh);
    }

    private void reload() {
        Azure.az(IAzureAccount.class).account();
        final long syncTime = this.syncTimeRef.get();
        final R remote = this.remoteRef.get();
        if (Objects.isNull(remote) && StringUtils.equals(this.statusRef.get(), Status.CREATING)) {
            return;
        }
        this.doModify(() -> {
            try {
                final R refreshed = syncTime > 0 && Objects.nonNull(remote) ? this.refreshRemote() : null;
                return Objects.nonNull(refreshed) ? refreshed : this.getModule().loadResourceFromAzure(this.name, this.resourceGroupName);
            } catch (ManagementException e) {
                if (HttpStatus.SC_NOT_FOUND == e.getResponse().getStatusCode()) {
                    return null;
                }
                throw e;
            }
        }, Status.LOADING);
    }

    @Override
    public AzResource.Draft<T, R> update() {
        return this.getModule().update(this.<T>cast(this));
    }

    @Override
    public void delete() {
        assert this.exists();
        this.doModify(() -> {
            this.getModule().deleteResourceFromAzure(this.getId());
            this.setStatus(Status.DELETED);
            this.getModule().deleteResourceFromLocal(name);
            return null;
        }, Status.DELETING);
    }

    protected synchronized void setRemote(@Nullable R newRemote) {
        final R oldRemote = this.remoteRef.get();
        if (Objects.equals(oldRemote, newRemote) && Objects.isNull(newRemote)) {
            return;
        }
        if (this.syncTimeRef.get() > 0 && Objects.equals(oldRemote, newRemote)) {
            this.setStatus(this.loadStatus(newRemote));
        } else {
            this.syncTimeRef.set(System.currentTimeMillis());
            this.remoteRef.set(newRemote);
            if (Objects.nonNull(newRemote)) {
                this.doModifyAsync(() -> this.setStatus(this.loadStatus(newRemote)), Status.LOADING);
            } else {
                this.setStatus(Status.DISCONNECTED);
            }
        }
    }

    @Override
    @Nullable
    public final R getRemote() {
        if (this.syncTimeRef.get() < 0) {
            this.reload();
        }
        return this.remoteRef.get();
    }

    protected synchronized void setStatus(@Nonnull String status) {
        // TODO: state engine to manage status, e.g. DRAFT -> CREATING
        final String oldStatus = this.statusRef.get();
        if (!Objects.equals(oldStatus, status)) {
            this.statusRef.set(status);
            AzureEventBus.emit("resource.status_changed.resource", this);
        }
    }

    @Nonnull
    public String getStatus() {
        final R remote = this.getRemote();
        final String status = this.statusRef.get();
        if (Objects.nonNull(remote) && Objects.isNull(status)) {
            this.setStatus(this.loadStatus(remote));
        }
        return status;
    }

    @Override
    public String getFormalStatus() {
        return this.formalizeStatus(this.getStatus());
    }

    protected void doModify(Runnable body, String status) {
        // TODO: lock so that can not modify if modifying.
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        try {
            body.run();
            this.refreshRemote();
            this.setRemote(this.remoteRef.get());
        } catch (Throwable t) {
            this.setStatus(Status.UNKNOWN);
            throw t;
        }
    }

    protected R refreshRemote() {
        final R remote = this.remoteRef.get();
        if (remote instanceof Refreshable) {
            // noinspection unchecked
            return ((Refreshable<R>) remote).refresh();
        }
        return null;
    }

    protected void doModifyAsync(Runnable body, String status) {
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.doModify(body, status));
    }

    protected R doModify(Callable<R> body, String status) {
        // TODO: lock so that can not modify if modifying.
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        try {
            final R remote = body.call();
            this.setRemote(remote);
            return remote;
        } catch (Throwable t) {
            this.setStatus(Status.UNKNOWN);
            throw new AzureToolkitRuntimeException(t);
        }
    }

    protected void doModifyAsync(Callable<R> body, String status) {
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.doModify(body, status));
    }

    @Nonnull
    public String getId() {
        return this.getModule().toResourceId(this.name, this.resourceGroupName);
    }

    public abstract List<AzResourceModule<?, T, ?>> getSubModules();

    public String formalizeStatus(String status) {
        return status;
    }

    @Nonnull
    public abstract String loadStatus(@Nonnull R remote);

    protected Optional<R> remoteOptional() {
        return Optional.ofNullable(this.getRemote());
    }

    private <D> D cast(@Nonnull Object origin) {
        //noinspection unchecked
        return (D) origin;
    }
}
