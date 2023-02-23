/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.eventhubs.EventHubsManager;
import com.azure.resourcemanager.eventhubs.models.EventHub;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventHubsInstanceModule  extends AbstractAzResourceModule<EventHubsInstance, EventHubsNamespace, EventHub> {
    public static final String NAME = "eventhubs";
    public EventHubsInstanceModule(@Nonnull EventHubsNamespace parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    protected List<EventHub> getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(eventHubNamespace -> eventHubNamespace.listEventHubs()
                .stream().collect(Collectors.toList())).orElse(null);
    }

    @Nullable
    @Override
    protected EventHub loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final List<EventHub> eventHubList = Optional.ofNullable(this.getClient()).orElse(Collections.emptyList());
        return eventHubList.stream().filter(instance -> name.equals(instance.name())).findAny().orElse(null);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, EventHub>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    @Override
    protected Stream<EventHub> loadResourcesFromAzure() {
        List<EventHub> eventHubList = Optional.ofNullable(this.getClient()).orElse(Collections.emptyList());
        return eventHubList.stream();
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.parent.getParent().getRemote()).map(EventHubsManager::eventHubs)
                .ifPresent(client -> client.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected EventHubsInstance newResource(@Nonnull EventHub remote) {
        return new EventHubsInstance(remote, this);
    }

    @Nonnull
    @Override
    protected EventHubsInstance newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new EventHubsInstance(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Event Hubs Instance";
    }
}
