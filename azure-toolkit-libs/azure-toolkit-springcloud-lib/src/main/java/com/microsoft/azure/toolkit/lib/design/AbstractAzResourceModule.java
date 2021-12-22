/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design;

import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsGettingById;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsDeletingById;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListing;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public abstract class AbstractAzResourceModule<T extends AbstractAzResource<T, P, R>, P extends AzResource<P, ?, ?>, R> implements AzResourceModule<T, P> {
    @Nonnull
    private final String name;
    @Nonnull
    protected final P parent;
    private boolean refreshed;
    @Getter(AccessLevel.NONE)
    private final Map<String, T> resources = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public List<T> list() {
        if (!this.refreshed) {
            this.refresh();
        }
        return new ArrayList<>(this.resources.values());
    }

    @Override
    public T get(@Nonnull String name, String resourceGroup) {
        if (!this.resources.containsKey(name)) {
            try {
                final T resource = wrap(loadResourceFromAzure(toResourceId(name, resourceGroup)));
                resource.refresh();
                this.resources.putIfAbsent(name, resource);
            } catch (Throwable e) { // TODO: handle exception
                return null;
            }
        }
        return this.resources.get(name);
    }

    @Override
    public T create(@Nonnull String name, String resourceGroup, Object config) { // TODO: add async one
        if (this.resources.containsKey(name)) {
            throw new AzureToolkitRuntimeException(String.format("resource named \"%s\" is existent", name));
        }
        T wrapper = this.createNewResource(name, resourceGroup, config);
        try {
            wrapper.doModify(() -> {
                final R remote = this.createResourceInAzure(name, resourceGroup, config);
                wrapper.setRemote(remote);
            });
        } catch (Throwable e) { // TODO: handle exception
        }
        this.resources.putIfAbsent(name, wrapper);
        return wrapper;
    }

    @Override
    public void update(@Nonnull String name, String resourceGroup, Object config) { // TODO: add async one
        final T wrapper = this.get(name, resourceGroup);
        try {
            assert wrapper != null;
            wrapper.doModify(() -> {
                final R remote = this.updateResourceInAzure(wrapper.getRemote(), config);
                wrapper.setRemote(remote);
            });
        } catch (Throwable e) { // TODO: handle exception
        }
    }

    @Override
    public void delete(@Nonnull String name, String resourceGroup) { // TODO: add async one
        final T wrapper = this.get(name, resourceGroup);
        try {
            assert wrapper != null;
            wrapper.doModify(() -> {
                this.deleteResourceFromAzure(toResourceId(name, resourceGroup));
                this.resources.remove(name);
            });
        } catch (Throwable ignored) { // TODO: handle exception
        }
    }

    @Override
    public synchronized void refresh() {
        try {
            this.refreshed = false;
            this.loadResourcesFromAzure().parallel().map(this::wrap).forEach(m -> this.resources.putIfAbsent(m.getName(), m));
            this.resources.values().stream().parallel().forEach(AzResource::refresh);
            this.refreshed = true;
        } catch (Throwable t) { // TODO: handle exception
            this.refreshed = false;
        }
    }

    @Nonnull
    public String toResourceId(@Nonnull String name, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("%s/%s/%s", this.parent.getId(), this.getName(), name).replace(AzResource.RESOURCE_GROUP_PLACEHOLDER, rg);
    }

    protected Stream<R> loadResourcesFromAzure() {
        if (this.getClient() instanceof SupportsListing) {
            return ((SupportsListing<R>) this.getClient()).list().stream();
        }
        throw new AzureToolkitRuntimeException("`getClient()` is not implemented");
    }

    protected R loadResourceFromAzure(@Nonnull String resourceId) {
        if (this.getClient() instanceof SupportsGettingById) {
            return ((SupportsGettingById<R>) this.getClient()).getByIdAsync(resourceId).block();
        }
        throw new AzureToolkitRuntimeException("`getClient()` is not implemented");
    }

    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        if (this.getClient() instanceof SupportsDeletingById) {
            ((SupportsDeletingById) this.getClient()).deleteById(resourceId);
        }
        throw new AzureToolkitRuntimeException("`getClient()` is not implemented");
    }

    protected abstract R createResourceInAzure(String name, String resourceGroup, Object config);

    protected abstract R updateResourceInAzure(R remote, Object config);

    protected abstract T createNewResource(String name, String resourceGroup, Object config);

    protected abstract T wrap(R r);

    protected abstract Object getClient();
}
