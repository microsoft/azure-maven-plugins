/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingById;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingByName;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingByResourceGroup;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsDeletingById;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListing;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.IResourceManager;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource.Status;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractAzResourceModule<T extends AbstractAzResource<T, P, R>, P extends AbstractAzResource<P, ?, ?>, R> implements AzResourceModule<T, P, R> {
    @Nonnull
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String name;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final P parent;
    @ToString.Include
    @Getter(AccessLevel.NONE)
    private long syncTime = -1;
    @Getter(AccessLevel.NONE)
    private final Map<String, Optional<T>> resources = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public synchronized List<T> list() {
        Azure.az(IAzureAccount.class).account();
        if (this.syncTime < 0) {
            this.reload();
        }
        return this.resources.values().stream().filter(Optional::isPresent).map(Optional::get)
            .sorted(Comparator.comparing(AbstractAzResource::getName)).collect(Collectors.toList());
    }

    @Nullable
    @Override
    public T get(@Nonnull String name, String resourceGroup) {
        Azure.az(IAzureAccount.class).account();
        if (!this.resources.containsKey(name)) {
            R remote = null;
            try {
                remote = loadResourceFromAzure(name, resourceGroup);
            } catch (ManagementException e) {
                if (HttpStatus.SC_NOT_FOUND != e.getResponse().getStatusCode()) {
                    throw e;
                }
            }
            if (Objects.isNull(remote)) {
                this.addResourceToLocal(name, null);
            } else {
                final T resource = newResource(remote);
                resource.setRemote(remote);
                this.addResourceToLocal(name, resource);
            }
        }
        return this.resources.get(name).orElse(null);
    }

    @Override
    public boolean exists(@NotNull String name, String resourceGroup) {
        final T resource = this.get(name, resourceGroup);
        return Objects.nonNull(resource) && resource.exists();
    }

    @Override
    public void delete(@Nonnull String name, String resourceGroup) {
        final T resource = this.get(name, resourceGroup);
        if (Objects.nonNull(resource)) {
            resource.delete();
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" doesn't exist", name));
    }

    @Nonnull
    public T getOrDraft(@Nonnull String name, String resourceGroup) {
        return Optional.ofNullable(this.get(name, resourceGroup)).orElseGet(() -> this.newDraft(name, resourceGroup));
    }

    public <D extends AzResource.Draft<T, R>> D updateOrCreate(String name, String resourceGroup) {
        return this.cast(this.newDraft(name, resourceGroup));
    }

    @Nonnull
    public <D extends AzResource.Draft<T, R>> D create(@Nonnull String name, String resourceGroup) {
        final T resource = this.get(name, resourceGroup);
        if (Objects.isNull(resource)) {
            // TODO: use generics to avoid class casting
            return this.cast(this.newDraftForCreate(name, resourceGroup));
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" is existing", name));
    }

    @Override
    public T create(@NotNull AzResource.Draft<T, R> draft) {
        final T existing = this.get(draft.getName(), draft.getResourceGroupName());
        if (Objects.isNull(existing)) {
            final T resource = cast(draft);
            // this will notify azure explorer to show a draft resource first
            this.addResourceToLocal(resource.getName(), resource);
            resource.doModify(draft::createResourceInAzure, Status.CREATING);
            return resource;
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" is existing", existing.getName()));
    }

    @Nonnull
    <D extends AzResource.Draft<T, R>> D update(@Nonnull T resource) {
        if (resource instanceof AzResource.Draft) {
            return this.cast(resource);
        }
        final T draft = this.newDraftForUpdate(resource);
        this.<T>cast(draft).setRemote(resource.getRemote());
        return this.cast(draft);
    }

    @Override
    public T update(@NotNull AzResource.Draft<T, R> draft) {
        final T resource = this.get(draft.getName(), draft.getResourceGroupName());
        if (Objects.nonNull(resource) && Objects.nonNull(resource.getRemote())) {
            this.<T>cast(draft).setRemote(resource.getRemote());
            resource.doModify(() -> draft.updateResourceInAzure(resource.getRemote()), Status.UPDATING);
            this.<T>cast(draft).setRemote(resource.getRemote());
            return resource;
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" doesn't exist", draft.getName()));
    }

    @Override
    public void refresh() {
        this.syncTime = -1;
        fireResourcesChangedEvent();
    }

    private synchronized void reload() {
        Stream<R> loaded;
        try {
            loaded = this.loadResourcesFromAzure();
        } catch (Throwable t) {
            this.syncTime = -1;
            throw t;
        }
        final Map<String, T> loadedResources = loaded.parallel().map(remote -> {
            final T resource = this.newResource(remote);
            resource.setRemote(remote);
            return resource;
        }).collect(Collectors.toMap(AbstractAzResource::getName, r -> r));
        final Set<String> localResources = this.resources.values().stream().filter(Optional::isPresent).map(Optional::get)
            .map(AbstractAzResource::getName).collect(Collectors.toSet());
        final Set<String> creating = this.resources.values().stream().filter(Optional::isPresent).map(Optional::get)
            .filter(r -> Status.CREATING.equals(r.getStatus())).map(AbstractAzResource::getName).collect(Collectors.toSet());
        final Sets.SetView<String> refreshed = Sets.intersection(localResources, loadedResources.keySet());
        final Sets.SetView<String> deleted = Sets.difference(Sets.difference(localResources, loadedResources.keySet()), creating);
        final Sets.SetView<String> added = Sets.difference(loadedResources.keySet(), localResources);

        refreshed.forEach(name -> this.resources.get(name).ifPresent(r -> r.setRemote(loadedResources.get(name).getRemote())));
        deleted.forEach(name -> Optional.ofNullable(this.deleteResourceFromLocal(name)).ifPresent(t -> t.setStatus(Status.DELETED)));
        added.forEach(name -> this.addResourceToLocal(name, loadedResources.get(name)));
        this.syncTime = System.currentTimeMillis();
    }

    @Nonnull
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("%s/%s/%s", this.parent.getId(), this.getName(), resourceName).replace(AzResource.RESOURCE_GROUP_PLACEHOLDER, rg);
    }

    protected String fixResourceGroup(String resourceGroup) {
        resourceGroup = StringUtils.equalsAnyIgnoreCase(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER) ? null : resourceGroup;
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        return resourceGroup;
    }

    @Nullable
    T deleteResourceFromLocal(@Nonnull String name) {
        final Optional<T> removed = this.resources.remove(name);
        if (Objects.nonNull(removed) && removed.isPresent()) {
            fireResourcesChangedEvent();
        }
        return removed.orElse(null);
    }

    private synchronized void addResourceToLocal(@Nonnull String name, @Nullable T resource) {
        if (!this.resources.getOrDefault(name, Optional.empty()).isPresent()) {
            this.resources.put(name, Optional.ofNullable(resource));
            fireResourcesChangedEvent();
        }
    }

    private void fireResourcesChangedEvent() {
        if (this.getParent() instanceof IResourceManager) {
            final AzResourceModule<P, ?, ?> service = this.getParent().getModule();
            AzureEventBus.emit("service.children_changed.service", service);
        }
        if (this instanceof AzService) {
            AzureEventBus.emit("service.children_changed.service", this);
        }
        AzureEventBus.emit("resource.children_changed.resource", this.getParent());
        AzureEventBus.emit("module.children_changed.module", this);
    }

    @Nonnull
    protected Stream<R> loadResourcesFromAzure() {
        final Object client = this.getClient();
        if (client instanceof SupportsListing) {
            return this.<SupportsListing<R>>cast(client).list().stream();
        } else {
            return Stream.empty();
        }
    }

    @Nullable
    protected R loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        final Object client = this.getClient();
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        resourceGroup = StringUtils.equals(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER) ? null : resourceGroup;
        if (client instanceof SupportsGettingByName) {
            return this.<SupportsGettingByName<R>>cast(client).getByName(name);
        } else if (client instanceof SupportsGettingByResourceGroup && StringUtils.isNotEmpty(resourceGroup)) {
            return this.<SupportsGettingByResourceGroup<R>>cast(client).getByResourceGroup(resourceGroup, name);
        } else if (client instanceof SupportsGettingById && StringUtils.isNotEmpty(resourceGroup)) {
            return this.<SupportsGettingById<R>>cast(client).getByIdAsync(toResourceId(name, resourceGroup)).block();
        } else { // fallback to filter the named resource from all resources in current module.
            return this.list().stream().filter(r -> StringUtils.equals(name, r.getName())).findAny().map(AbstractAzResource::getRemote).orElse(null);
        }
    }

    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final Object client = this.getClient();
        if (client instanceof SupportsDeletingById) {
            ((SupportsDeletingById) client).deleteById(resourceId);
        }
    }

    /**
     * @param <D> type of draft, it must extend {@link D} and implement {@link AzResource.Draft}
     */
    protected <D extends T> D newDraft(@Nonnull String name, String resourceGroup) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    /**
     * @param <D> type of draft, it must extend {@link D} and implement {@link AzResource.Draft}
     */
    protected <D extends T> D newDraftForCreate(@Nonnull String name, String resourceGroup) {
        return this.newDraft(name, resourceGroup);
    }

    /**
     * @param <D> type of draft, it must extend {@link D} and implement {@link AzResource.Draft}
     */
    protected <D extends T> D newDraftForUpdate(@Nonnull T t) {
        return this.newDraft(t.getName(), t.getResourceGroupName());
    }

    protected abstract T newResource(@Nonnull R r);

    /**
     * get track2 client, which is used to implement {@link #loadResourcesFromAzure}, {@link #loadResourceFromAzure} and {@link #deleteResourceFromAzure}
     */
    protected Object getClient() {
        throw new AzureToolkitRuntimeException("not implemented");
    }

    private <D> D cast(@Nonnull Object origin) {
        //noinspection unchecked
        return (D) origin;
    }
}
