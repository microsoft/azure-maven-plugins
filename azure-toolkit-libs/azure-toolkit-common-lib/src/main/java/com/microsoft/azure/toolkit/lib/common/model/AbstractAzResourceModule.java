/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingById;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsDeletingById;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListing;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public abstract class AbstractAzResourceModule<T extends AbstractAzResource<T, P, R>, P extends AzResource<P, ?, ?>, R> implements AzResourceModule<T, P> {
    @Nonnull
    private final String name;
    @Nonnull
    protected final P parent;
    @Getter(AccessLevel.NONE)
    private long syncTime;
    @Getter(AccessLevel.NONE)
    private final Map<String, T> resources = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public List<T> list() {
        if (this.syncTime < 0) {
            this.refresh();
        }
        return new ArrayList<>(this.resources.values());
    }

    @Nonnull
    public T getOrInit(@Nonnull String name, String resourceGroup) {
        return Optional.ofNullable(this.get(name, resourceGroup)).orElse(this.init(name, resourceGroup));
    }

    @Nonnull
    public T getOrNew(@Nonnull String name, String resourceGroup) {
        return Optional.ofNullable(this.get(name, resourceGroup)).orElse(this.newResource(name, resourceGroup));
    }

    @Nullable
    @Override
    public T get(@Nonnull String name, String resourceGroup) {
        if (!this.resources.containsKey(name)) {
            try {
                final R remote = loadResourceFromAzure(toResourceId(name, resourceGroup));
                final T resource = newResource(remote);
                resource.setRemote(remote);
                this.resources.putIfAbsent(name, resource);
            } catch (Throwable e) { // TODO: handle exception
                return null;
            }
        }
        return this.resources.get(name);
    }

    @Nonnull
    @Override
    public T init(@Nonnull String name, String resourceGroup) { // TODO: add async one
        if (this.resources.containsKey(name)) {
            throw new AzureToolkitRuntimeException(String.format("resource named \"%s\" is existent", name));
        }
        T wrapper = this.newResource(name, resourceGroup);
        this.resources.putIfAbsent(name, wrapper);
        return wrapper;
    }

    @Override
    public void delete(@Nonnull String name, String resourceGroup) { // TODO: add async one
        final T wrapper = this.get(name, resourceGroup);
        try {
            assert wrapper != null;
            wrapper.delete();
        } catch (Throwable ignored) { // TODO: handle exception
        }
    }

    @Override
    public synchronized void refresh() {
        try {
            this.syncTime = -1;
            final Map<String, T> newResources = this.loadResourcesFromAzure().parallel().map(remote -> {
                final T resource = this.newResource(remote);
                resource.setRemote(remote);
                return resource;
            }).collect(Collectors.toMap(AbstractAzResource::getName, r -> r));
            final Sets.SetView<String> toRemove = Sets.difference(this.resources.keySet(), newResources.keySet());
            final Sets.SetView<String> toAdd = Sets.difference(newResources.keySet(), this.resources.keySet());
            toAdd.forEach(m -> this.resources.put(m, newResources.get(m)));
            toRemove.forEach(this.resources::remove);
            this.syncTime = System.currentTimeMillis();
        } catch (Throwable t) { // TODO: handle exception
            this.syncTime = -1;
            throw new AzureToolkitRuntimeException(t);
        }
    }

    @Nonnull
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("%s/%s/%s", this.parent.getId(), this.getName(), resourceName).replace(AzResource.RESOURCE_GROUP_PLACEHOLDER, rg);
    }

    protected void deleteResourceFromLocal(@Nonnull String name) {
        this.resources.remove(name);
    }

    protected void addResourceToLocal(@Nonnull T resource) {
        this.resources.putIfAbsent(resource.getName(), resource);
    }

    protected Stream<R> loadResourcesFromAzure() {
        final Object client = this.getClient();
        if (client instanceof SupportsListing) {
            return ((SupportsListing<R>) client).list().stream();
        }
        throw new AzureToolkitRuntimeException("`getClient()` is not implemented");
    }

    protected R loadResourceFromAzure(@Nonnull String resourceId) {
        final Object client = this.getClient();
        if (client instanceof SupportsGettingById) {
            return ((SupportsGettingById<R>) client).getByIdAsync(resourceId).block();
        }
        throw new AzureToolkitRuntimeException("`getClient()` is not implemented");
    }

    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final Object cliet = this.getClient();
        if (cliet instanceof SupportsDeletingById) {
            ((SupportsDeletingById) cliet).deleteById(resourceId);
        }
        throw new AzureToolkitRuntimeException("`getClient()` is not implemented");
    }

    public abstract T newResource(@Nonnull String name, @Nonnull String resourceGroup);

    protected abstract T newResource(R r);

    protected abstract R createResourceInAzure(@Nonnull String name, @Nonnull String resourceGroup, Object config);

    protected abstract R updateResourceInAzure(@Nonnull R remote, Object config);

    protected abstract Object getClient();
}
