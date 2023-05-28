/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingById;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingByName;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingByResourceGroup;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsDeletingById;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListing;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.resource.GenericResource;
import com.microsoft.azure.toolkit.lib.resource.GenericResourceModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupModule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractAzResourceModule<T extends AbstractAzResource<T, P, R>, P extends AzResource, R>
    implements AzResourceModule<T> {
    @Getter
    @Nonnull
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String name;
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include
    protected final P parent;
    @Nonnull
    @ToString.Include
    private final AtomicLong syncTimeRef = new AtomicLong(-1);
    @Nonnull
    private final Map<String, Optional<T>> resources = Collections.synchronizedMap(new LinkedHashMap<>());

    @Nonnull
    private final Debouncer fireEvents = new TailingDebouncer(this::fireChildrenChangedEvent, 300);
    private final Lock lock = new ReentrantLock();
    private Iterator<? extends ContinuablePage<String, R>> pages;

    @Override
    public void refresh() {
        log.debug("[{}]:refresh()", this.name);
        this.invalidateCache();
        AzureEventBus.emit("module.refreshed.module", this);
        AzureEventBus.emit("resource.children_changed.resource", this.getParent());
    }

    protected void invalidateCache() {
        log.debug("[{}]:invalidateCache()", this.name);
        if (this.lock.tryLock()) {
            try {
                this.resources.entrySet().removeIf(e -> !e.getValue().isPresent());
                this.syncTimeRef.set(-1);
            } finally {
                this.lock.unlock();
            }
        }
        log.debug("[{}]:invalidateCache->resources.invalidateCache()", this.name);
        final Collection<Optional<T>> values = new ArrayList<>(this.resources.values());
        values.forEach(v -> v.ifPresent(AbstractAzResource::invalidateCache));
    }

    @Nonnull
    @Override
    public List<T> list() { // getResources
        log.debug("[{}]:list()", this.name);
        if (isAuthRequired()) {
            Azure.az(IAzureAccount.class).account();
        }
        if (this.parent instanceof AbstractAzResource && ((AbstractAzResource<?, ?, ?>) this.parent).isDraftForCreating()) {
            log.debug("[{}]:list->parent.isDraftForCreating()=true", this.name);
            return Collections.emptyList();
        }
        if (this.syncTimeRef.get() < 1) { // 0, -1 or too old.
            try {
                this.lock.lock();
                if (this.syncTimeRef.get() == -1) { // -1 or too old.
                    log.debug("[{}]:list->this.reload()", this.name);
                    this.reloadResources();
                }
            } finally {
                this.lock.unlock();
            }
        }
        log.debug("[{}]:list->this.resources.values()", this.name);
        return this.resources.values().stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    private void reloadResources() {
        log.debug("[{}]:reloadResources()", this.name);
        this.syncTimeRef.set(0);
        try {
            log.debug("[{}]:reloadResources->loadResourcePagesFromAzure()", this.name);
            this.pages = this.loadResourcePagesFromAzure();
            final ContinuablePage<String, R> page = pages.hasNext() ? pages.next() : new ItemPage<>(Collections.emptyList());
            final Map<String, R> loadedResources = page.getElements().stream()
                .collect(Collectors.toMap(r -> this.newResource(r).getId().toLowerCase(), r -> r));
            log.debug("[{}]:reloadResources->setResources(xxx)", this.name);
            this.setResources(loadedResources);
        } catch (Exception e) {
            log.debug("[{}]:reloadResources->setResources([])", this.name);
            final Throwable cause = e instanceof HttpResponseException ? e : ExceptionUtils.getRootCause(e);
            if (cause instanceof HttpResponseException && HttpStatus.SC_NOT_FOUND == ((HttpResponseException) cause).getResponse().getStatusCode()) {
                log.debug("[{}]:reloadResources->loadResourceFromAzure()=SC_NOT_FOUND", this.name, e);
                this.setResources(Collections.emptyMap());
            } else {
                log.debug("[{}]:reloadResources->loadResourcePagesFromAzure()=EXCEPTION", this.name, e);
                this.resources.clear();
                this.syncTimeRef.compareAndSet(0, System.currentTimeMillis());
                throw e;
            }
        }
    }

    public void loadMoreResources() {
        log.debug("[{}]:loadMoreResources()", this.name);
        try {
            this.lock.lock();
            if (Objects.isNull(this.pages)) {
                this.reloadResources();
            } else if (this.pages.hasNext()) {
                final ContinuablePage<String, R> page = this.pages.next();
                final Map<String, R> loadedResources = page.getElements().stream()
                    .collect(Collectors.toMap(r -> this.newResource(r).getId().toLowerCase(), r -> r));
                log.debug("[{}]:loadMoreResources->addResources(xxx)", this.name);
                this.addResources(loadedResources);
                fireEvents.debounce();
            }
        } finally {
            this.lock.unlock();
        }
    }

    public boolean hasMoreResources() {
        return Objects.nonNull(this.pages) && this.pages.hasNext();
    }

    private void setResources(Map<String, R> loadedResources) {
        final Set<String> localResources = this.resources.values().stream().filter(Optional::isPresent).map(Optional::get)
            .map(AbstractAzResource::getId).map(String::toLowerCase).collect(Collectors.toSet());
        final Set<String> creating = this.resources.values().stream().filter(Optional::isPresent).map(Optional::get)
            .filter(AbstractAzResource::isDraftForCreating)
            .map(AbstractAzResource::getId).map(String::toLowerCase).collect(Collectors.toSet());
        log.debug("[{}]:reload().creating={}", this.name, creating);
        final Sets.SetView<String> refreshed = Sets.intersection(localResources, loadedResources.keySet());
        log.debug("[{}]:reload().refreshed={}", this.name, refreshed);
        final Sets.SetView<String> deleted = Sets.difference(Sets.difference(localResources, loadedResources.keySet()), creating);
        log.debug("[{}]:reload().deleted={}", this.name, deleted);
        final Sets.SetView<String> added = Sets.difference(loadedResources.keySet(), localResources);
        log.debug("[{}]:reload().added={}", this.name, added);
        log.debug("[{}]:reload.deleted->deleteResourceFromLocal", this.name);
        deleted.forEach(id -> this.resources.getOrDefault(id, Optional.empty()).ifPresent(r -> {
            r.deleteFromCache();
            r.setRemote(null);
        }));

        final AzureTaskManager m = AzureTaskManager.getInstance();
        log.debug("[{}]:reload.refreshed->resource.setRemote", this.name);
        refreshed.forEach(id -> this.resources.getOrDefault(id, Optional.empty()).ifPresent(r -> m.runOnPooledThread(() -> r.setRemote(loadedResources.get(id)))));
        log.debug("[{}]:reload.added->addResourceToLocal", this.name);
        added.stream().map(loadedResources::get).map(r -> Pair.of(r, this.newResource(r)))
            .sorted(Comparator.comparing(p -> p.getValue().getName())) // sort by name when adding into cache
            .forEach(p -> {
                final R remote = p.getKey();
                final T resource = p.getValue();
                m.runOnPooledThread(() -> resource.setRemote(remote));
                this.addResourceToLocal(resource.getId(), resource, true);
            });
        this.syncTimeRef.set(System.currentTimeMillis());
    }

    private void addResources(Map<String, R> loadedResources) {
        final Set<String> added = loadedResources.keySet();
        log.debug("[{}]:reload().added={}", this.name, added);
        loadedResources.values().stream().map(r -> Pair.of(r, this.newResource(r)))
            .sorted(Comparator.comparing(p -> p.getValue().getName())) // sort by name when adding into cache
            .forEach(p -> {
                final R remote = p.getKey();
                final T resource = p.getValue();
                AzureTaskManager.getInstance().runOnPooledThread(() -> resource.setRemote(remote));
                this.addResourceToLocal(resource.getId(), resource, true);
            });
        this.syncTimeRef.set(System.currentTimeMillis());
    }

    public void clear() {
        log.debug("[{}]:clear()", this.name);
        try {
            this.lock.lock();
            this.resources.clear();
            this.syncTimeRef.set(-1);
        } finally {
            this.lock.unlock();
        }
    }

    @Nullable
    @Override
    public T get(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:get({}, {})", this.name, name, resourceGroup);
        if (StringUtils.isBlank(name) || (this.parent instanceof AbstractAzResource && ((AbstractAzResource<?, ?, ?>) this.parent).isDraftForCreating())) {
            log.debug("[{}]:get->parent.isDraftForCreating()=true||isBlank(name)=true", this.name);
            return null;
        }
        if (isAuthRequired()) {
            Azure.az(IAzureAccount.class).account();
        }
        final String id = this.toResourceId(name, resourceGroup).toLowerCase();
        if (!this.resources.containsKey(id)) {
            R remote = null;
            try {
                log.debug("[{}]:get({}, {})->loadResourceFromAzure()", this.name, name, resourceGroup);
                remote = loadResourceFromAzure(name, resourceGroup);
            } catch (Exception e) {
                log.debug("[{}]:get({}, {})->loadResourceFromAzure()=EXCEPTION", this.name, name, resourceGroup, e);
                final Throwable cause = e instanceof HttpResponseException ? e : ExceptionUtils.getRootCause(e);
                if (cause instanceof HttpResponseException) {
                    if (HttpStatus.SC_NOT_FOUND != ((HttpResponseException) cause).getResponse().getStatusCode()) {
                        log.debug("[{}]:get({}, {})->loadResourceFromAzure()=SC_NOT_FOUND", this.name, name, resourceGroup, e);
                        throw e;
                    }
                }
            }
            if (Objects.isNull(remote)) {
                log.debug("[{}]:get({}, {})->addResourceToLocal({}, null)", this.name, name, resourceGroup, name);
                this.addResourceToLocal(id, null, true);
            } else {
                final T resource = newResource(remote);
                resource.setRemote(remote);
                log.debug("[{}]:get({}, {})->addResourceToLocal({}, resource)", this.name, name, resourceGroup, name);
                this.addResourceToLocal(resource.getId(), resource, true);
            }
        }
        log.debug("[{}]:get({}, {})->this.resources.get({})", this.name, id, resourceGroup, name);
        return this.resources.getOrDefault(id, Optional.empty()).orElse(null);
    }

    @Nullable
    public T get(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        log.debug("[{}]:get({})", this.name, resourceId);
        return this.get(id.name(), id.resourceGroupName());
    }

    @Override
    public boolean exists(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:exists({}, {})", this.name, name, resourceGroup);
        final T resource = this.get(name, resourceGroup);
        return Objects.nonNull(resource) && resource.exists();
    }

    @Override
    public void delete(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:delete({}, {})", this.name, name, resourceGroup);
        log.debug("[{}]:delete->this.get({}, {})", this.name, name, resourceGroup);
        final T resource = this.get(name, resourceGroup);
        if (Objects.nonNull(resource)) {
            log.debug("[{}]:delete->resource.delete()", this.name);
            resource.delete();
        } else {
            throw new AzureToolkitRuntimeException(String.format("resource \"%s\" doesn't exist", name));
        }
    }

    @Nonnull
    public T getOrDraft(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:getOrDraft({}, {})", this.name, name, resourceGroup);
        return Optional.ofNullable(this.get(name, resourceGroup)).orElseGet(() -> this.cast(this.newDraftForCreate(name, resourceGroup)));
    }

    @Nonnull
    public T getOrTemp(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:getOrTemp({}, {})", this.name, name, rgName);
        final String id = this.toResourceId(name, resourceGroup).toLowerCase();
        return this.resources.getOrDefault(id, Optional.empty()).orElseGet(() -> this.newResource(name, resourceGroup));
    }

    @Nonnull
    public T getOrInit(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:getOrDraft({}, {})", this.name, name, rgName);
        final String id = this.toResourceId(name, resourceGroup).toLowerCase();
        return this.resources.getOrDefault(id, Optional.empty()).orElseGet(() -> {
            final T resource = this.newResource(name, resourceGroup);
            log.debug("[{}]:get({}, {})->addResourceToLocal({}, resource)", this.name, id, resourceGroup, name);
            this.addResourceToLocal(id, resource);
            return resource;
        });
    }

    @Nonnull
    public List<T> listCachedResources() { // getResources
        return this.resources.values().stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    @Nonnull
    public List<T> listByResourceGroup(@Nonnull String resourceGroup) {
        log.debug("[{}]:listByResourceGroupName({})", this.name, resourceGroup);
        return this.list().stream().filter(r -> r.getResourceGroupName().equalsIgnoreCase(resourceGroup)).collect(Collectors.toList());
    }

    @Nonnull
    public <D extends AzResource.Draft<T, R>> D updateOrCreate(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:updateOrCreate({}, {})", this.name, name, resourceGroup);
        final T resource = this.get(name, resourceGroup);
        if (Objects.nonNull(resource)) {
            return this.cast(this.newDraftForUpdate(resource));
        }
        return this.cast(this.newDraftForCreate(name, resourceGroup));
    }

    @Nonnull
    public <D extends AzResource.Draft<T, R>> D create(@Nonnull String name, @Nullable String rgName) {
        final String resourceGroup = normalizeResourceGroupName(name, rgName);
        log.debug("[{}]:create({}, {})", this.name, name, resourceGroup);
        // TODO: use generics to avoid class casting
        log.debug("[{}]:create->newDraftForCreate({}, {})", this.name, name, resourceGroup);
        return this.cast(this.newDraftForCreate(name, resourceGroup));
    }

    @Nonnull
    @Override
    public T create(@Nonnull AzResource.Draft<T, ?> d) {
        final AzResource.Draft<T, R> draft = this.cast(d);
        log.debug("[{}]:create(draft:{})", this.name, draft);
        final T existing = this.get(draft.getName(), draft.getResourceGroupName());
        if (Objects.isNull(existing)) {
            final T resource = cast(draft);
            // this will notify azure explorer to show a draft resource first
            log.debug("[{}]:create->addResourceToLocal({})", this.name, resource);
            this.addResourceToLocal(resource.getId(), resource);
            AzureEventBus.emit("resource.creation_started.resource", resource);
            log.debug("[{}]:create->doModify(draft.createResourceInAzure({}))", this.name, resource);
            try {
                resource.doModify(draft::createResourceInAzure, AzResource.Status.CREATING);
            } catch (RuntimeException e) {
                resource.delete();
                throw e;
            }
            AzureEventBus.emit("azure.explorer.highlight_resource", resource);
            return resource;
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" is existing", existing.getName()));
    }

    @Nonnull
    <D extends AzResource.Draft<T, R>> D update(@Nonnull T resource) {
        log.debug("[{}]:update(resource:{})", this.name, resource);
        if (resource.isDraftForCreating()) {
            log.warn("[{}]:updating(resource:{}) from a draftForCreating", this.name, resource);
        }
        if (resource.isDraftForUpdating()) {
            return this.cast(resource);
        }
        log.debug("[{}]:update->newDraftForUpdate({})", this.name, resource);
        final T draft = this.cast(this.newDraftForUpdate(resource));
        return this.cast(draft);
    }

    @Nonnull
    @Override
    public T update(@Nonnull AzResource.Draft<T, ?> draft) {
        final AzResource.Draft<T, R> d = this.cast(draft);
        log.debug("[{}]:update(draft:{})", this.name, draft);
        final T resource = this.get(draft.getName(), draft.getResourceGroupName());
        if (Objects.nonNull(resource) && Objects.nonNull(resource.getRemote())) {
            log.debug("[{}]:update->doModify(draft.updateResourceInAzure({}))", this.name, resource.getRemote());
            resource.doModify(() -> d.updateResourceInAzure(Objects.requireNonNull(resource.getRemote())), AzResource.Status.UPDATING);
            return resource;
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" doesn't exist", draft.getName()));
    }

    @Nonnull
    @SneakyThrows(UnsupportedEncodingException.class)
    public String toResourceId(@Nonnull String resourceName, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName(), RESOURCE_GROUP_PLACEHOLDER);
        // resource (ACR repository) name may contain "/".
        final String encoded = URLEncoder.encode(resourceName, "UTF-8");
        return String.format("%s/%s/%s", this.parent.getId(), this.getName(), encoded).replace(RESOURCE_GROUP_PLACEHOLDER, resourceGroup);
    }

    protected void deleteResourceFromLocal(@Nonnull String id, boolean... silent) {
        log.debug("[{}]:deleteResourceFromLocal({})", this.name, id);
        log.debug("[{}]:deleteResourceFromLocal->this.resources.remove({})", this.name, id);
        id = id.toLowerCase();
        final Optional<T> removed = this.resources.remove(id);
        if (Objects.nonNull(removed) && removed.isPresent()) {
            this.deleteResourceFromLocalResourceGroup(removed.get(), silent);
            if ((silent.length == 0 || !silent[0])) {
                log.debug("[{}]:deleteResourceFromLocal->fireResourcesChangedEvent()", this.name);
                fireEvents.debounce();
            }
        }
    }

    protected void deleteResourceFromLocalResourceGroup(@Nonnull T resource, boolean... silent) {
        final ResourceId rId = ResourceId.fromString(resource.getId());
        final ResourceGroup resourceGroup = resource.getResourceGroup();
        if (Objects.isNull(rId.parent()) && Objects.nonNull(resourceGroup) &&
            !(resource instanceof ResourceGroup) && !(resource instanceof ResourceDeployment)) {
            final GenericResourceModule genericResourceModule = resourceGroup.genericResources();
            genericResourceModule.deleteResourceFromLocal(resource.getId(), silent);
        }
    }

    protected void addResourceToLocal(@Nonnull String id, @Nullable T resource, boolean... silent) {
        log.debug("[{}]:addResourceToLocal({}, {})", this.name, id, resource);
        id = id.toLowerCase();
        final Optional<T> oldResource = this.resources.getOrDefault(id, Optional.empty());
        final Optional<T> newResource = Optional.ofNullable(resource);
        if (!oldResource.isPresent()) {
            log.debug("[{}]:addResourceToLocal->this.resources.put({}, {})", this.name, id, resource);
            this.resources.put(id, newResource);
            if (newResource.isPresent()) {
                this.addResourceToLocalResourceGroup(id, resource, silent);
                if (silent.length == 0 || !silent[0]) {
                    log.debug("[{}]:addResourceToLocal->fireResourcesChangedEvent()", this.name);
                    fireEvents.debounce();
                }
            }
        }
    }

    protected void addResourceToLocalResourceGroup(@Nonnull String id, @Nonnull T resource, boolean... silent) {
        final ResourceId rId = ResourceId.fromString(id);
        final ResourceGroup resourceGroup = resource.getResourceGroup();
        if (Objects.isNull(rId.parent()) && Objects.nonNull(resourceGroup) &&
            !(resource instanceof ResourceGroup) && !(resource instanceof ResourceDeployment)) {
            final GenericResourceModule genericResourceModule = resourceGroup.genericResources();
            final GenericResource genericResource = genericResourceModule.newResource(resource);
            genericResourceModule.addResourceToLocal(resource.getId(), genericResource, silent);
        }
    }

    private void fireChildrenChangedEvent() {
        log.debug("[{}]:fireChildrenChangedEvent()", this.name);
        if (this.getParent() instanceof AbstractAzServiceSubscription) {
            @SuppressWarnings("unchecked") final AzResourceModule<P> service = (AzResourceModule<P>) this.getParent().getModule();
            AzureEventBus.emit("service.children_changed.service", service);
        }
        if (this instanceof AzService) {
            AzureEventBus.emit("service.children_changed.service", this);
        }
        AzureEventBus.emit("resource.children_changed.resource", this.getParent());
        AzureEventBus.emit("module.children_changed.module", this);
    }

    @Nonnull
    @AzureOperation(name = "azure/resource.load_resources_by_page.type", params = {"this.getResourceTypeName()"})
    protected Iterator<? extends ContinuablePage<String, R>> loadResourcePagesFromAzure() {
        log.debug("[{}]:loadPagedResourcesFromAzure()", this.getName());
        final Object client = this.getClient();
        if (!this.parent.exists()) {
            return Collections.emptyIterator();
        } else if (client instanceof SupportsListing) {
            log.debug("[{}]:loadPagedResourcesFromAzure->client.list()", this.name);
            return this.<SupportsListing<R>>cast(client).list().iterableByPage(getPageSize()).iterator();
        } else {
            log.debug("[{}]:loadPagedResourcesFromAzure->NOT Supported", this.name);
            throw new AzureToolkitRuntimeException("not supported");
        }
    }

    @Nullable
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected R loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        log.debug("[{}]:loadResourceFromAzure({}, {})", this.getName(), name, resourceGroup);
        final Object client = this.getClient();
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        resourceGroup = StringUtils.equals(resourceGroup, RESOURCE_GROUP_PLACEHOLDER) ? null : resourceGroup;
        if (client instanceof SupportsGettingById && StringUtils.isNotEmpty(resourceGroup)) {
            log.debug("[{}]:loadResourceFromAzure->client.getById({}, {})", this.name, resourceGroup, name);
            return this.<SupportsGettingById<R>>cast(client).getById(toResourceId(name, resourceGroup));
        } else if (client instanceof SupportsGettingByResourceGroup && StringUtils.isNotEmpty(resourceGroup)) {
            log.debug("[{}]:loadResourceFromAzure->client.getByResourceGroup({}, {})", this.name, resourceGroup, name);
            return this.<SupportsGettingByResourceGroup<R>>cast(client).getByResourceGroup(resourceGroup, name);
        } else if (client instanceof SupportsGettingByName) {
            log.debug("[{}]:loadResourceFromAzure->client.getByName({})", this.name, name);
            return this.<SupportsGettingByName<R>>cast(client).getByName(name);
        } else { // fallback to filter the named resource from all resources in current module.
            log.debug("[{}]:loadResourceFromAzure->this.list().filter({}).getRemote()", this.name, name);
            return this.list().stream().filter(r -> StringUtils.equals(name, r.getName())).findAny().map(AbstractAzResource::getRemote).orElse(null);
        }
    }

    @AzureOperation(name = "azure/resource.delete_resource.resource|type", params = {"nameFromResourceId(resourceId)", "this.getResourceTypeName()"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        log.debug("[{}]:deleteResourceFromAzure({})", this.getName(), resourceId);
        final Object client = this.getClient();
        if (client instanceof SupportsDeletingById) {
            log.debug("[{}]:deleteResourceFromAzure->client.deleteById({})", this.name, resourceId);
            ((SupportsDeletingById) client).deleteById(resourceId);
        }
    }

    private String normalizeResourceGroupName(String name, @Nullable String rgName) {
        rgName = StringUtils.firstNonBlank(rgName, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(rgName) || StringUtils.equalsIgnoreCase(rgName, RESOURCE_GROUP_PLACEHOLDER)) {
            if (this instanceof ResourceGroupModule) {
                return name;
            } else if (this instanceof AzService) {
                return RESOURCE_GROUP_PLACEHOLDER;
            }
            throw new IllegalArgumentException("Resource Group name is required for " + this.getResourceTypeName());
        }
        return rgName;
    }

    @Nonnull
    protected AzResource.Draft<T, R> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    protected AzResource.Draft<T, R> newDraftForUpdate(@Nonnull T t) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    protected abstract T newResource(@Nonnull R r);

    @Nonnull
    protected abstract T newResource(@Nonnull String name, @Nullable String resourceGroupName);

    /**
     * get track2 client, which is used to implement {@link #loadResourcePagesFromAzure}, {@link #loadResourceFromAzure} and {@link #deleteResourceFromAzure}
     */
    @Nullable
    protected Object getClient() {
        throw new AzureToolkitRuntimeException("not implemented");
    }

    @Override
    @Nonnull
    public String getFullResourceType() {
        return this.getParent().getFullResourceType() + "/" + this.getName();
    }

    @Nonnull
    public String getResourceTypeName() {
        return this.getFullResourceType();
    }

    @Nonnull
    public String getSubscriptionId() {
        return this.getParent().getSubscriptionId();
    }

    @Nonnull
    protected <D> D cast(@Nonnull Object origin) {
        //noinspection unchecked
        return (D) origin;
    }

    public static int getPageSize() {
        return Azure.az().config().getPageSize();
    }

    protected boolean isAuthRequired() {
        return true;
    }
}
