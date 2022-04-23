/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.resources.fluentcore.model.Refreshable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.GenericResourceModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
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
    @Nonnull
    @ToString.Include
    final AtomicLong syncTimeRef; // 0:loading, <0:invalidated
    @Nonnull
    @ToString.Include
    final AtomicReference<String> statusRef;
    @Nonnull
    private final Debouncer fireEvents = new TailingDebouncer(this::fireStatusChangedEvent, 300);

    protected AbstractAzResource(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        this.name = name;
        this.resourceGroupName = resourceGroupName;
        this.module = module;
        this.remoteRef = new AtomicReference<>();
        this.syncTimeRef = new AtomicLong(-1);
        this.statusRef = new AtomicReference<>(Status.UNKNOWN);
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

    public boolean exists() {
        final P parent = this.getParent();
        if (StringUtils.equals(this.statusRef.get(), Status.DELETED)) {
            return false;
        } else if (parent == AzResource.NONE || this instanceof AbstractAzServiceSubscription || this instanceof ResourceGroup) {
            return this.remoteOptional().isPresent();
        } else {
            final ResourceGroup rg = this.getResourceGroup();
            return Objects.nonNull(rg) && rg.exists() && parent.exists() && this.remoteOptional().isPresent();
        }
    }

    @Override
    public void refresh() {
        log.debug("[{}:{}]:refresh()", this.module.getName(), this.getName());
        this.syncTimeRef.set(-1);
        log.debug("[{}:{}]:refresh->subModules.refresh()", this.module.getName(), this.getName());
        this.getSubModules().forEach(AzResourceModule::refresh);
        AzureEventBus.emit("resource.refreshed.resource", this);
    }

    @Override
    @Nullable
    public final R getRemote() {
        synchronized (this.syncTimeRef) {
            while (this.syncTimeRef.get() == 0) {
                try {
                    this.syncTimeRef.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.syncTimeRef.set(-1);
                }
            }
            if (this.syncTimeRef.compareAndSet(-1, 0)) {
                log.debug("[{}:{}]:getRemote->reload()", this.module.getName(), this.getName());
                this.reloadRemote();
            }
            return this.remoteRef.get();
        }
    }

    @AzureOperation(name = "resource.reload.resource|type", params = {"this.getName()", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    private void reloadRemote() {
        log.debug("[{}:{}]:reload()", this.module.getName(), this.getName());
        if (this.isDraftForCreating()) {
            synchronized (this.syncTimeRef) {
                this.syncTimeRef.compareAndSet(0, -1);
                this.syncTimeRef.notifyAll();
            }
            return;
        }
        Azure.az(IAzureAccount.class).account();
        final R remote = this.remoteRef.get();
        this.doModify(() -> {
            log.debug("[{}:{}]:reload->this.refreshRemote()", this.module.getName(), this.getName());
            final R refreshed = Objects.nonNull(remote) ? this.refreshRemote(remote) : null;
            log.debug("[{}:{}]:reload->this.loadRemote()", this.module.getName(), this.getName());
            return Objects.nonNull(refreshed) ? refreshed : this.loadRemote();
        }, Status.LOADING);
    }

    @Nullable
    protected final R loadRemote() {
        log.debug("[{}:{}]:reloadRemote()", this.module.getName(), this.getName());
        try {
            return this.getModule().loadResourceFromAzure(this.getName(), this.getResourceGroupName());
        } catch (Exception e) {
            log.debug("[{}:{}]:reload->this.refreshRemote/loadResourceFromAzure=EXCEPTION", this.module.getName(), this.getName(), e);
            final Throwable cause = e instanceof ManagementException ? e : ExceptionUtils.getRootCause(e);
            if (cause instanceof ManagementException && HttpStatus.SC_NOT_FOUND == ((ManagementException) cause).getResponse().getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    @Nullable
    protected R refreshRemote(@Nonnull R remote) {
        log.debug("[{}:{}]:refreshRemote()", this.module.getName(), this.getName());
        if (remote instanceof Refreshable) {
            log.debug("[{}:{}]:refreshRemote->remote.refresh()", this.module.getName(), this.getName());
            // noinspection unchecked
            return ((Refreshable<R>) remote).refresh();
        } else {
            log.debug("[{}:{}]:refreshRemote->reloadRemote()", this.module.getName(), this.getName());
            return this.loadRemote();
        }
    }

    protected void setRemote(@Nullable R newRemote) {
        synchronized (this.syncTimeRef) {
            log.debug("[{}:{}]:setRemote({})", this.module.getName(), this.getName(), newRemote);
            log.debug("[{}:{}]:setRemote->this.remoteRef.set({})", this.module.getName(), this.getName(), newRemote);
            this.remoteRef.set(newRemote);
            this.syncTimeRef.set(System.currentTimeMillis());
            this.syncTimeRef.notifyAll();
        }
        if (Objects.nonNull(newRemote)) {
            log.debug("[{}:{}]:setRemote->setStatus(LOADING)", this.module.getName(), this.getName());
            this.setStatus(Status.LOADING);
            log.debug("[{}:{}]:setRemote->this.reloadStatus", this.module.getName(), this.getName());
            AzureTaskManager.getInstance().runOnPooledThread(this::reloadStatus);
        } else {
            log.debug("[{}:{}]:setRemote->this.setStatus(DISCONNECTED)", this.module.getName(), this.getName());
            this.setStatus(Status.DELETED);
        }
    }

    @Nonnull
    @Override
    public AzResource.Draft<T, R> update() {
        log.debug("[{}:{}]:update()", this.module.getName(), this.getName());
        log.debug("[{}:{}]:update->module.update(this)", this.module.getName(), this.getName());
        return this.getModule().update(this.<T>cast(this));
    }

    @Override
    public void delete() {
        log.debug("[{}:{}]:delete()", this.module.getName(), this.getName());
        this.setStatus(Status.DELETING);
        if (this.exists()) {
            this.deleteFromAzure();
        }
        this.setRemote(null);
        this.deleteFromLocal();
        this.getSubModules().stream().flatMap(m -> m.list().stream()).forEach(AbstractAzResource::delete);
    }

    private void deleteFromAzure() {
        // TODO: set status should also cover its child
        log.debug("[{}:{}]:delete->module.deleteResourceFromAzure({})", this.module.getName(), this.getName(), this.getId());
        try {
            this.getModule().deleteResourceFromAzure(this.getId());
        } catch (Exception e) {
            final Throwable cause = e instanceof ManagementException ? e : ExceptionUtils.getRootCause(e);
            if (cause instanceof ManagementException && HttpStatus.SC_NOT_FOUND != ((ManagementException) cause).getResponse().getStatusCode()) {
                log.debug("[{}]:delete()->deleteResourceFromAzure()=SC_NOT_FOUND", this.name, e);
            } else {
                this.getSubModules().stream().flatMap(m -> m.list().stream()).forEach(r -> r.setStatus(Status.UNKNOWN));
                throw e;
            }
        }
    }

    public void deleteFromLocal() {
        log.debug("[{}:{}]:delete->this.setStatus(DELETED)", this.module.getName(), this.getName());
        this.setStatus(Status.DELETED);
        log.debug("[{}:{}]:delete->module.deleteResourceFromLocal({})", this.module.getName(), this.getName(), this.getName());
        this.getModule().deleteResourceFromLocal(this.getName());
        final ResourceId id = ResourceId.fromString(this.getId());
        if (Objects.isNull(id.parent()) &&
            !StringUtils.equalsIgnoreCase(id.subscriptionId(), NONE.getName()) &&
            !StringUtils.equalsAnyIgnoreCase(id.resourceGroupName(), NONE.getName(), RESOURCE_GROUP_PLACEHOLDER)) { // resource group manages top resources only
            final ResourceGroup resourceGroup = this.getResourceGroup();
            if (Objects.nonNull(resourceGroup)) {
                final GenericResourceModule genericResourceModule = resourceGroup.genericResources();
                ((AbstractAzResourceModule) genericResourceModule).deleteResourceFromLocal(this.getId());
            }
        }
    }

    public void setStatus(@Nonnull String status) {
        synchronized (this.statusRef) {
            log.debug("[{}:{}]:setStatus({})", this.module.getName(), this.getName(), status);
            // TODO: state engine to manage status, e.g. DRAFT -> CREATING
            final String oldStatus = this.statusRef.get();
            if (!Objects.equals(oldStatus, status)) {
                this.statusRef.set(status);
                if (StringUtils.equalsAny(status, Status.DELETING, Status.DELETED)) {
                    this.getSubModules().stream().flatMap(m -> m.list().stream()).forEach(r -> r.setStatus(status));
                }
                if (!StringUtils.equalsIgnoreCase(status, Status.LOADING)) {
                    this.statusRef.notifyAll();
                }
                fireEvents.debounce();
            }
        }
    }

    @AzureOperation(
        name = "resource.reload_status.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    private void reloadStatus() {
        log.debug("[{}:{}]:reloadStatus()", this.module.getName(), this.getName());
        try {
            log.debug("[{}:{}]:reloadStatus->loadStatus()", this.module.getName(), this.getName());
            this.remoteOptional().map(this::loadStatus).ifPresent(this::setStatus);
        } catch (Throwable t) {
            log.debug("[{}:{}]:reloadStatus->loadStatus()=EXCEPTION", this.module.getName(), this.getName(), t);
            this.setStatus(Status.UNKNOWN);
        }
    }

    @Nonnull
    public String getStatus() {
        final String status = this.statusRef.get();
        if ((this.syncTimeRef.get() < 0)) {
            log.debug("[{}:{}]:getStatus->reloadStatus()", this.module.getName(), this.getName());
            AzureTaskManager.getInstance().runOnPooledThread(this::reloadStatus);
            return this.statusRef.get();
        }
        return status;
    }

    @Nonnull
    public String getStatusSync() {
        synchronized (this.statusRef) {
            String status = this.statusRef.get();
            while (StringUtils.equalsIgnoreCase(status, Status.LOADING)) {
                try {
                    this.statusRef.wait();
                    status = this.statusRef.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (this.syncTimeRef.get() < 0) {
                log.debug("[{}:{}]:getStatusSync->reloadStatus()", this.module.getName(), this.getName());
                this.reloadStatus();
                return this.statusRef.get();
            }
            return status;
        }
    }

    protected void doModify(@Nonnull Runnable body, @Nullable String status) {
        // TODO: lock so that can not modify if modifying.
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        try {
            body.run();
            log.debug("[{}:{}]:doModify->refreshRemote()", this.module.getName(), this.getName());
            final R refreshed = Optional.ofNullable(this.remoteRef.get()).map(this::refreshRemote).orElse(null);
            log.debug("[{}:{}]:doModify->setRemote({})", this.module.getName(), this.getName(), this.remoteRef.get());
            this.setRemote(refreshed);
        } catch (Throwable t) {
            this.setStatus(Status.UNKNOWN);
            synchronized (this.syncTimeRef) {
                this.syncTimeRef.compareAndSet(0, -1);
                this.syncTimeRef.notifyAll();
            }
            throw t;
        }
    }

    protected void doModifyAsync(@Nonnull Runnable body, @Nullable String status) {
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.doModify(body, status));
    }

    @Nullable
    public R doModify(@Nonnull Callable<R> body, @Nullable String status) {
        // TODO: lock so that can not modify if modifying.
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        try {
            final R remote = body.call();
            log.debug("[{}:{}]:doModify->setRemote({})", this.module.getName(), this.getName(), remote);
            this.setRemote(remote);
            return remote;
        } catch (Throwable t) {
            this.setStatus(Status.UNKNOWN);
            synchronized (this.syncTimeRef) {
                this.syncTimeRef.compareAndSet(0, -1);
                this.syncTimeRef.notifyAll();
            }
            throw new AzureToolkitRuntimeException(t);
        }
    }

    protected void doModifyAsync(@Nonnull Callable<R> body, @Nullable String status) {
        this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.doModify(body, status));
    }

    private void fireStatusChangedEvent() {
        log.debug("[{}]:fireStatusChangedEvent()", this.getName());
        AzureEventBus.emit("resource.status_changed.resource", this);
    }

    @Nonnull
    public String getId() {
        if (this.remoteRef.get() instanceof HasId) {
            return ((HasId) this.remoteRef.get()).id();
        }
        return this.getModule().toResourceId(this.getName(), this.getResourceGroupName());
    }

    @Nonnull
    public abstract List<AbstractAzResourceModule<?, T, ?>> getSubModules();

    @Nonnull
    public abstract String loadStatus(@Nonnull R remote);

    @Nonnull
    protected Optional<R> remoteOptional() {
        return Optional.ofNullable(this.getRemote());
    }

    @Nonnull
    private <D> D cast(@Nonnull Object origin) {
        //noinspection unchecked
        return (D) origin;
    }

    @Nullable
    public AbstractAzResourceModule<?, T, ?> getSubModule(String moduleName) {
        return this.getSubModules().stream().filter(m -> m.getName().equals(moduleName)).findAny().orElse(null);
    }

    @Nullable
    public ResourceGroup getResourceGroup() {
        final String rgName = this.getResourceGroupName();
        if (StringUtils.equalsAnyIgnoreCase(rgName, None.NONE, AzResource.RESOURCE_GROUP_PLACEHOLDER)) {
            return null;
        }
        return Azure.az(AzureResources.class).groups(this.getSubscriptionId()).get(rgName, rgName);
    }

    public boolean isDraft() {
        return this.isDraftForCreating() || this.isDraftForUpdating();
    }

    public boolean isDraftForCreating() {
        return this instanceof Draft && Objects.isNull(((Draft<?, ?>) this).getOrigin()) && Objects.isNull(this.remoteRef.get());
    }

    public boolean isDraftForUpdating() {
        return this instanceof Draft && Objects.nonNull(((Draft<?, ?>) this).getOrigin());
    }
}
