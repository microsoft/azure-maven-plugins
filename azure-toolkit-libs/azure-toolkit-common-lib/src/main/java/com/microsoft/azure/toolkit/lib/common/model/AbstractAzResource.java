/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.exception.HttpResponseException;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.resources.fluentcore.model.Refreshable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractAzResource<T extends AbstractAzResource<T, P, R>, P extends AzResource, R> implements AzResource {
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
    @ToString.Include
    private final AtomicLong syncTimeRef; // 0:loading, <0:invalidated
    @Nonnull
    private final AtomicReference<R> remoteRef;
    @Nonnull
    @ToString.Include
    private final AtomicReference<String> statusRef;
    @Nonnull
    private final Debouncer fireEvents = new TailingDebouncer(this::fireStatusChangedEvent, 300);
    private final Lock lock = new ReentrantLock();

    protected AbstractAzResource(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        this.name = name;
        this.resourceGroupName = resourceGroupName;
        this.module = module;
        this.remoteRef = new AtomicReference<>();
        this.statusRef = new AtomicReference<>(Status.UNKNOWN);
        this.syncTimeRef = new AtomicLong(-1);
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
    protected AbstractAzResource(@Nonnull AbstractAzResource<T, P, R> origin) {
        this.name = origin.getName();
        this.resourceGroupName = origin.getResourceGroupName();
        this.module = origin.getModule();
        this.remoteRef = origin.remoteRef;
        this.statusRef = origin.statusRef;
        this.syncTimeRef = origin.syncTimeRef;
    }

    public boolean exists() {
        final P parent = this.getParent();
        if (this.isEmulatorResource()) {
            return true;
        } else if (StringUtils.equals(this.statusRef.get(), Status.DELETED)) {
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
        this.invalidateCache();
        AzureEventBus.emit("resource.refreshed.resource", this);
    }

    public void invalidateCache() {
        log.debug("[{}:{}]:invalidateCache->subModules.invalidateCache()", this.module.getName(), this.getName());
        this.getSubModules().forEach(AbstractAzResourceModule::invalidateCache);
        log.debug("[{}]:invalidateCache()", this.name);
        if (this.lock.tryLock()) {
            try {
                // this.remoteRef.set(null); will make a newly created resource behave as a "draft for creating"(since isDraftForCreating() will return true)
                this.syncTimeRef.set(-1);
            } finally {
                this.lock.unlock();
            }
        }
    }

    @Nullable
    public final R getRemote(boolean... sync) {
        log.debug("[{}:{}]:getRemote()", this.module.getName(), this.getName());
        if (!isEmulatorResource()) {
            Azure.az(IAzureAccount.class).account();
        }
        if (this.isDraftForCreating()) {
            log.debug("[{}:{}]:getRemote->this.isDraftForCreating()=true", this.module.getName(), this.getName());
            return null;
        }
        if (sync.length > 0 && sync[0]) {
            try {
                this.lock.lock();
                return this.getRemoteInner();
            } finally {
                this.lock.unlock();
            }
        } else {
            return this.getRemoteInner();
        }
    }

    private R getRemoteInner() {
        if (System.currentTimeMillis() - this.syncTimeRef.get() > AzResource.CACHE_LIFETIME) { // 0, -1 or too old.
            final R remote = this.remoteRef.get();
            if (this.syncTimeRef.get() == 0 && Objects.nonNull(remote)) {
                return remote;
            }
            try {
                this.lock.lock();
                if (this.syncTimeRef.get() != 0 && System.currentTimeMillis() - this.syncTimeRef.get() > AzResource.CACHE_LIFETIME) { // -1 or too old.
                    log.debug("[{}:{}]:getRemote->reloadRemote()", this.module.getName(), this.getName());
                    this.reloadRemote();
                }
            } finally {
                this.lock.unlock();
            }
        }
        return this.remoteRef.get();
    }

    @AzureOperation(name = "azure/resource.reload_resource.resource|type", params = {"this.getName()", "this.getResourceTypeName()"})
    private void reloadRemote() {
        log.debug("[{}:{}]:reloadRemote()", this.module.getName(), this.getName());
        this.doModify(() -> {
            log.debug("[{}:{}]:reloadRemote->this.refreshRemote()", this.module.getName(), this.getName());
            final R oldRemote = this.remoteRef.get();
            final R refreshed = Objects.nonNull(oldRemote) ? this.refreshRemote(oldRemote) : null;
            log.debug("[{}:{}]:reloadRemote->this.loadRemote()", this.module.getName(), this.getName());
            final R remote = Objects.nonNull(refreshed) ? refreshed : this.loadRemote();
            if (Objects.isNull(remote)) {
                this.deleteFromCache();
            }
            return remote;
        }, Status.LOADING);
    }

    protected void setRemote(@Nullable R newRemote) {
        final R oldRemote = this.remoteRef.get();
        log.debug("[{}:{}]:setRemote({})", this.module.getName(), this.getName(), newRemote);
        if (oldRemote == null || newRemote == null) {
            log.debug("[{}:{}]:setRemote->subModules.invalidateCache()", this.module.getName(), this.getName());
            this.getSubModules().forEach(AbstractAzResourceModule::invalidateCache);
        }
        if (this.lock.tryLock()) {
            try {
                log.debug("[{}:{}]:setRemote->this.remoteRef.set({})", this.module.getName(), this.getName(), newRemote);
                this.remoteRef.set(newRemote);
                this.syncTimeRef.set(System.currentTimeMillis());
                if (Objects.nonNull(newRemote)) {
                    log.debug("[{}:{}]:setRemote->setStatus(LOADING)", this.module.getName(), this.getName());
                    this.setStatus(Status.LOADING);
                    log.debug("[{}:{}]:setRemote->this.loadStatus", this.module.getName(), this.getName());
                    this.updateAdditionalProperties(newRemote, oldRemote);
                    Optional.of(newRemote).map(this::loadStatus).ifPresent(this::setStatus);
                } else {
                    log.debug("[{}:{}]:setRemote->this.setStatus(DISCONNECTED)", this.module.getName(), this.getName());
                    this.updateAdditionalProperties(null, oldRemote);
                    this.setStatus(Status.DELETED);
                    this.getSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(r -> r.setRemote(null));
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

    protected void updateAdditionalProperties(@Nullable R newRemote, @Nullable R oldRemote) {

    }

    @Nullable
    protected final R loadRemote() {
        log.debug("[{}:{}]:loadRemote()", this.module.getName(), this.getName());
        try {
            return this.getModule().loadResourceFromAzure(this.getName(), this.getResourceGroupName());
        } catch (Exception e) {
            log.debug("[{}:{}]:loadRemote()=EXCEPTION", this.module.getName(), this.getName(), e);
            final Throwable cause = e instanceof HttpResponseException ? e : ExceptionUtils.getRootCause(e);
            if (cause instanceof HttpResponseException && HttpStatus.SC_NOT_FOUND == ((HttpResponseException) cause).getResponse().getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    /**
     * @return null if resource has been deleted.
     */
    @Nullable
    private R refreshRemote(@Nonnull R remote) {
        try {
            return this.refreshRemoteFromAzure(remote);
        } catch (Exception e) {
            log.debug("[{}:{}]:refreshRemoteFromAzure()=EXCEPTION", this.module.getName(), this.getName(), e);
            final Throwable cause = e instanceof HttpResponseException ? e : ExceptionUtils.getRootCause(e);
            if (cause instanceof HttpResponseException && HttpStatus.SC_NOT_FOUND == ((HttpResponseException) cause).getResponse().getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    /**
     * @return null if resource has been deleted.
     */
    @Nullable
    protected R refreshRemoteFromAzure(@Nonnull R remote) {
        log.debug("[{}:{}]:refreshRemoteFromAzure()", this.module.getName(), this.getName());
        if (remote instanceof Refreshable) {
            log.debug("[{}:{}]:refreshRemoteFromAzure->remote.refresh()", this.module.getName(), this.getName());
            // noinspection unchecked
            return ((Refreshable<R>) remote).refresh();
        } else {
            log.debug("[{}:{}]:refreshRemoteFromAzure->reloadRemote()", this.module.getName(), this.getName());
            return this.loadRemote();
        }
    }

    @Nonnull
    public AzResource.Draft<T, R> update() {
        log.debug("[{}:{}]:update()", this.module.getName(), this.getName());
        log.debug("[{}:{}]:update->module.update(this)", this.module.getName(), this.getName());
        return this.getModule().update(this.<T>cast(this));
    }

    @Override
    public void delete() {
        log.debug("[{}:{}]:delete()", this.module.getName(), this.getName());
        this.doModify(() -> {
            if (this.exists()) {
                this.deleteFromAzure();
            }
            return null;
        }, Status.DELETING);
        this.deleteFromCache();
    }

    private void deleteFromAzure() {
        // TODO: set status should also cover its child
        log.debug("[{}:{}]:delete->module.deleteResourceFromAzure({})", this.module.getName(), this.getName(), this.getId());
        try {
            this.getModule().deleteResourceFromAzure(this.getId());
        } catch (Exception e) {
            final Throwable cause = e instanceof HttpResponseException ? e : ExceptionUtils.getRootCause(e);
            if (cause instanceof HttpResponseException && HttpStatus.SC_NOT_FOUND == ((HttpResponseException) cause).getResponse().getStatusCode()) {
                log.debug("[{}]:delete()->deleteResourceFromAzure()=SC_NOT_FOUND", this.name, e);
            } else {
                this.getSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(r -> r.setStatus(Status.UNKNOWN));
                throw e;
            }
        }
    }

    public void deleteFromCache() {
        log.debug("[{}:{}]:delete->this.setStatus(DELETED)", this.module.getName(), this.getName());
        this.setStatus(Status.DELETED);
        log.debug("[{}:{}]:delete->module.deleteResourceFromLocal({})", this.module.getName(), this.getName(), this.getName());
        this.getModule().deleteResourceFromLocal(this.getId());
        final ResourceId id = ResourceId.fromString(this.getId());
        final ResourceGroup resourceGroup = this.getResourceGroup();
        if (Objects.isNull(id.parent()) && Objects.nonNull(resourceGroup)) { // resource group manages top resources only
            final GenericResourceModule genericResourceModule = resourceGroup.genericResources();
            genericResourceModule.deleteResourceFromLocal(this.getId());
        }
        this.getSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(AbstractAzResource::deleteFromCache);
    }

    public void reloadStatus() {
        this.setStatus(this.loadStatus(this.getRemote()));
    }

    public void setStatus(@Nonnull String status) {
        synchronized (this.statusRef) {
            log.debug("[{}:{}]:setStatus({})", this.module.getName(), this.getName(), status);
            // TODO: state engine to manage status, e.g. DRAFT -> CREATING
            final String oldStatus = this.statusRef.get();
            if (!Objects.equals(oldStatus, status)) {
                this.statusRef.set(status);
                fireEvents.debounce();
                if (StringUtils.equalsAny(status, Status.DELETING, Status.DELETED)) {
                    this.getSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(r -> r.setStatus(status));
                }
            }
        }
    }

    @Nonnull
    public String getStatus(boolean immediately) {
        if (this.syncTimeRef.get() == -1) {
            log.debug("[{}:{}]:getStatus->getStatusSync()", this.module.getName(), this.getName());
            if (immediately) {
                AzureTaskManager.getInstance().runOnPooledThread(this::getRemote);
            } else {
                this.getRemote();
            }
        }
        return this.statusRef.get();
    }

    protected void doModify(@Nonnull Runnable body, @Nullable String status) {
        if (this.lock.tryLock()) {
            try {
                this.syncTimeRef.set(0);
                this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
                log.debug("[{}:{}]:doModify->body.run()", this.module.getName(), this.getName());
                body.run();
                log.debug("[{}:{}]:doModify->refreshRemote()", this.module.getName(), this.getName());
                final R refreshed = Optional.ofNullable(this.remoteRef.get()).map(this::refreshRemote).orElse(null);
                log.debug("[{}:{}]:doModify->setRemote({})", this.module.getName(), this.getName(), this.remoteRef.get());
                this.setRemote(refreshed);
            } catch (Throwable t) {
                final Throwable cause = t instanceof HttpResponseException ? t : ExceptionUtils.getRootCause(t);
                if (cause instanceof HttpResponseException && HttpStatus.SC_NOT_FOUND == ((HttpResponseException) cause).getResponse().getStatusCode()) {
                    this.setRemote(null);
                } else {
                    this.syncTimeRef.compareAndSet(0, System.currentTimeMillis());
                    this.setStatus(Status.UNKNOWN);
                    throw t;
                }
            } finally {
                this.lock.unlock();
            }
        } else {
            AzureMessager.getMessager().warning(AzureString.format("%s (%s) is %s, please wait until it's finished.", this.getResourceTypeName(), this.getName(), this.getStatus()));
        }
    }

    @Nullable
    public R doModify(@Nonnull Callable<R> body, @Nullable String status) {
        if (!this.lock.tryLock()) {
            AzureMessager.getMessager().warning(AzureString.format("%s (%s) is %s, waiting until it's finished.", this.getResourceTypeName(), this.getName(), this.getStatus()));
            this.lock.lock();
        }
        try {
            this.syncTimeRef.set(0);
            this.setStatus(Optional.ofNullable(status).orElse(Status.PENDING));
            log.debug("[{}:{}]:doModify->body.call()", this.module.getName(), this.getName());
            final R remote = body.call();
            log.debug("[{}:{}]:doModify->setRemote({})", this.module.getName(), this.getName(), remote);
            this.setRemote(remote);
            return remote;
        } catch (Throwable t) {
            final Throwable cause = t instanceof HttpResponseException ? t : ExceptionUtils.getRootCause(t);
            if (cause instanceof HttpResponseException && HttpStatus.SC_NOT_FOUND == ((HttpResponseException) cause).getResponse().getStatusCode()) {
                this.setRemote(null);
                return null;
            } else {
                this.syncTimeRef.compareAndSet(0, System.currentTimeMillis());
                this.setStatus(Status.UNKNOWN);
                throw t instanceof AzureToolkitRuntimeException ? (AzureToolkitRuntimeException) t : new AzureToolkitRuntimeException(t);
            }
        } finally {
            this.lock.unlock();
        }
    }

    private void fireStatusChangedEvent() {
        log.debug("[{}]:fireStatusChangedEvent()", this.getName());
        AzureEventBus.emit("resource.status_changed.resource", this);
    }

    @Nonnull
    public String getId() {
        final R r = this.remoteRef.get();
        if (r instanceof HasId) {
            return ((HasId) r).id();
        }
        return this.getModule().toResourceId(this.getName(), this.getResourceGroupName());
    }

    @Nonnull
    public abstract List<AbstractAzResourceModule<?, ?, ?>> getSubModules();

    @Nonnull
    public abstract String loadStatus(@Nonnull R remote);

    @Nonnull
    protected Optional<R> remoteOptional(boolean... sync) {
        return Optional.ofNullable(this.getRemote(sync));
    }

    @Nonnull
    private <D> D cast(@Nonnull Object origin) {
        //noinspection unchecked
        return (D) origin;
    }

    @Nullable
    public AbstractAzResourceModule<?, ?, ?> getSubModule(String moduleName) {
        return this.getSubModules().stream().filter(m -> m.getName().equalsIgnoreCase(moduleName)).findAny().orElse(null);
    }

    @Nullable
    public ResourceGroup getResourceGroup() {
        final String rgName = this.getResourceGroupName();
        final String sid = this.getSubscriptionId();
        final boolean isSubscriptionSet = StringUtils.isNotBlank(sid) &&
            !StringUtils.equalsAnyIgnoreCase(sid, "<none>", NONE.getName());
        final boolean isResourceGroupSet = StringUtils.isNotBlank(rgName) &&
            !StringUtils.equalsAnyIgnoreCase(rgName, "<none>", NONE.getName(), RESOURCE_GROUP_PLACEHOLDER);
        if (!isResourceGroupSet || !isSubscriptionSet) {
            return null;
        }
        return Azure.az(AzureResources.class).groups(this.getSubscriptionId()).get(rgName, rgName);
    }

    @Nonnull
    public P getParent() {
        return this.getModule().getParent();
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

    public boolean isEmulatorResource() {
        return this.getParent() instanceof AbstractAzResource && ((AbstractAzResource<?, ?, ?>) this.getParent()).isEmulatorResource();
    }
}
