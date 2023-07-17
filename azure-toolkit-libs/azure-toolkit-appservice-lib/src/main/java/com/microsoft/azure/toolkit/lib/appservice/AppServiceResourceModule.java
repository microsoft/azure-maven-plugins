/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
public abstract class AppServiceResourceModule<T extends AppServiceAppBase<T, P, R>, P
    extends AbstractAzResource<P, ?, ?>, R extends WebAppBase> extends AbstractAzResourceModule<T, P, R> {

    public AppServiceResourceModule(@Nonnull String name, @Nonnull P parent) {
        super(name, parent);
    }

    @Override
    protected Map<String, R> getResourcesFromAzure() {
        final Map<String, R> result = new HashMap<>();
        loadResourceIdsFromAzure().forEach(id -> result.put(id, null));
        return result;
    }

    @Override
    protected void addResources(Map<String, R> loadedResources) {
        final Set<String> added = loadedResources.keySet();
        log.debug("[{}]:reload().added={}", this.getName(), added);
        loadedResources.entrySet().stream().map(this::convertToResourcePair)
            .sorted(Comparator.comparing(p -> p.getValue().getName())) // sort by name when adding into cache
            .forEach(p -> {
                final R remote = p.getKey();
                final T resource = p.getValue();
                final Runnable runnable = Objects.nonNull(remote) ? () -> resource.setRemote(remote) : resource::refresh;
                AzureTaskManager.getInstance().runOnPooledThread(runnable);
                this.addResourceToLocal(resource.getId(), resource, true);
            });
        this.syncTimeRef.set(System.currentTimeMillis());
    }

    protected Pair<R, T> convertToResourcePair(@Nonnull final Map.Entry<String, R> entry) {
        final String key = entry.getKey();
        final ResourceId resourceId = ResourceId.fromString(key);
        final R value = entry.getValue();
        final T result = Objects.nonNull(value) ? this.newResource(value) :
            this.newResource(resourceId.name(), resourceId.resourceGroupName());
        return Pair.of(value, result);
    }

    protected abstract List<String> loadResourceIdsFromAzure();
}
