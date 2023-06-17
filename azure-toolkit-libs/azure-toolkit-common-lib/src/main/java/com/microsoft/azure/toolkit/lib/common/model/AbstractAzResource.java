/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cache1;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
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
import java.util.concurrent.atomic.AtomicReference;

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
    private final Cache1<R> cache;
    @Nonnull
    @ToString.Include
    private final AtomicReference<String> status;
    @Nonnull
    private final Debouncer fireEvents = new TailingDebouncer(this::fireStatusChangedEvent, 300);

    protected AbstractAzResource(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        this.name = name;
        this.resourceGroupName = resourceGroupName;
        this.module = module;
        this.cache = new Cache1<>(this::loadRemoteFromAzure)
            .onValueChanged(this::onRemoteUpdated)
            .onStatusChanged(s -> fireEvents.debounce());
        this.status = new AtomicReference<>(Status.UNKNOWN);
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
        this.cache = origin.cache;
        this.status = origin.status;
    }

    public boolean exists() {
        final P parent = this.getParent();
        if (StringUtils.equals(this.status.get(), Status.DELETED)) {
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
        this.getCachedSubModules().forEach(AbstractAzResourceModule::invalidateCache);
        log.debug("[{}]:invalidateCache()", this.name);
        this.cache.invalidate();
    }

    @Nullable
    protected final R loadRemoteFromAzure() {
        log.debug("[{}:{}]:loadRemote()", this.module.getName(), this.getName());
        try {
            return this.getModule().loadResourceFromAzure(this.getName(), this.getResourceGroupName());
        } catch (final Exception e) {
            log.debug("[{}:{}]:loadRemote()=EXCEPTION", this.module.getName(), this.getName(), e);
            if (isNotFoundException(e)) {
                return null;
            }
            throw e;
        }
    }

    @Nullable
    public final R getRemote() {
        log.debug("[{}:{}]:getRemote()", this.module.getName(), this.getName());
        if (isAuthRequired()) {
            Azure.az(IAzureAccount.class).account();
        }
        if (this.isDraftForCreating()) {
            log.debug("[{}:{}]:getRemote->this.isDraftForCreating()=true", this.module.getName(), this.getName());
            return null;
        }
        return this.cache.get();
    }

    void setRemote(R remote) {
        this.cache.update(() -> remote, Status.UPDATING);
    }

    @Nonnull
    protected Optional<R> remoteOptional() {
        return Optional.ofNullable(this.getRemote());
    }

    protected void onRemoteUpdated(@Nullable R newRemote, R oldRemote) {
        log.debug("[{}:{}]:setRemote({})", this.module.getName(), this.getName(), newRemote);
        if (oldRemote == null || newRemote == null) {
            log.debug("[{}:{}]:setRemote->subModules.invalidateCache()", this.module.getName(), this.getName());
            this.getCachedSubModules().forEach(AbstractAzResourceModule::invalidateCache);
        }
        log.debug("[{}:{}]:setRemote->this.remoteRef.set({})", this.module.getName(), this.getName(), newRemote);
        if (Objects.nonNull(newRemote)) {
            log.debug("[{}:{}]:setRemote->setStatus(LOADING)", this.module.getName(), this.getName());
            log.debug("[{}:{}]:setRemote->this.loadStatus", this.module.getName(), this.getName());
            this.updateAdditionalProperties(newRemote, oldRemote);
            AzureTaskManager.getInstance().runOnPooledThread(() ->
                Optional.of(newRemote).map(this::loadStatus).ifPresent(this::setStatus));
        } else {
            log.debug("[{}:{}]:setRemote->this.setStatus(DISCONNECTED)", this.module.getName(), this.getName());
            this.deleteFromCache();
            this.updateAdditionalProperties(null, oldRemote);
            this.getCachedSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(r -> r.setRemote(null));
        }
    }

    protected void updateAdditionalProperties(@Nullable R newRemote, @Nullable R oldRemote) {

    }

    @Nonnull
    public String getStatus() {
        String cacheStatus = this.cache.getStatus();
        if (StringUtils.isBlank(cacheStatus)) {
            final R remote = this.cache.getIfPresent(true);
            cacheStatus = Optional.ofNullable(this.cache.getStatus()).orElse(Cache1.Status.LOADING);
        }
        return Cache1.Status.OK.equalsIgnoreCase(cacheStatus) ?
            Optional.ofNullable(this.status.get()).orElse(Cache1.Status.LOADING) : cacheStatus;
    }

    public void setStatus(@Nonnull String status) {
        synchronized (this.status) {
            log.debug("[{}:{}]:setStatus({})", this.module.getName(), this.getName(), status);
            // TODO: state engine to manage status, e.g. DRAFT -> CREATING
            final String oldStatus = this.status.get();
            if (!Objects.equals(oldStatus, status)) {
                this.status.set(status);
                fireEvents.debounce();
                if (StringUtils.equalsAny(status, Status.DELETING, Status.DELETED)) {
                    this.getCachedSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(r -> r.setStatus(status));
                }
            }
        }
    }

    @Nonnull
    protected abstract String loadStatus(@Nonnull R remote);

    private void fireStatusChangedEvent() {
        log.debug("[{}]:fireStatusChangedEvent()", this.getName());
        AzureEventBus.emit("resource.status_changed.resource", this);
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
        } catch (final Exception e) {
            if (isNotFoundException(e)) {
                log.debug("[{}]:delete()->deleteResourceFromAzure()=SC_NOT_FOUND", this.name, e);
            } else {
                this.getCachedSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(r -> r.setStatus(Status.UNKNOWN));
                throw e;
            }
        }
    }

    void deleteFromCache() {
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
        this.getCachedSubModules().stream().flatMap(m -> m.listCachedResources().stream()).forEach(AbstractAzResource::deleteFromCache);
    }

    @Nonnull
    public AzResource.Draft<T, R> update() {
        log.debug("[{}:{}]:update()", this.module.getName(), this.getName());
        log.debug("[{}:{}]:update->module.update(this)", this.module.getName(), this.getName());
        return this.getModule().update(this.<T>cast(this));
    }

    protected void doModify(@Nonnull Runnable body, @Nullable String status) {
        this.cache.update(body, status);
    }

    @Nullable
    protected R doModify(@Nonnull Callable<R> body, @Nullable String status) {
        return this.cache.update(body, status);
    }

    @Nonnull
    private <D> D cast(@Nonnull Object origin) {
        //noinspection unchecked
        return (D) origin;
    }

    @Nonnull
    public String getId() {
        final R r = this.cache.getIfPresent();
        if (r instanceof HasId) {
            return ((HasId) r).id();
        }
        return this.getModule().toResourceId(this.getName(), this.getResourceGroupName());
    }

    @Nonnull
    public abstract List<AbstractAzResourceModule<?, ?, ?>> getSubModules();

    @Nonnull
    protected List<AbstractAzResourceModule<?, ?, ?>> getCachedSubModules() {
        return getSubModules();
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
        return this instanceof Draft && Objects.isNull(((Draft<?, ?>) this).getOrigin()) && Objects.isNull(this.cache.getIfPresent());
    }

    public boolean isDraftForUpdating() {
        return this instanceof Draft && Objects.nonNull(((Draft<?, ?>) this).getOrigin());
    }

    protected boolean isAuthRequired() {
        return true;
    }

    static boolean isNotFoundException(Throwable t) {
        final Throwable cause = t instanceof HttpResponseException ? t : ExceptionUtils.getRootCause(t);
        return Optional.ofNullable(cause).filter(c -> cause instanceof HttpResponseException)
            .map(c -> ((HttpResponseException) c))
            .map(HttpResponseException::getResponse)
            .map(HttpResponse::getStatusCode)
            .filter(c -> c == HttpStatus.SC_NOT_FOUND)
            .isPresent();
    }
}
