/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.eventhubs.EventHubsManager;
import com.azure.resourcemanager.eventhubs.models.EventHubNamespace;
import com.azure.resourcemanager.eventhubs.models.EventHubNamespaces;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class EventHubsNamespaceModule extends AbstractAzResourceModule<EventHubsNamespace, EventHubsNamespaceSubscription, EventHubNamespace> {
    public static final String NAME = "namespaces";
    public EventHubsNamespaceModule(EventHubsNamespaceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, EventHubNamespace>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected EventHubNamespace loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(eventHubNamespaces -> eventHubNamespaces.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/eventhubs.delete_event_hubs_namespace.eventhubs", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(eventHubNamespaces -> eventHubNamespaces.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected EventHubsNamespace newResource(@Nonnull EventHubNamespace remote) {
        return new EventHubsNamespace(remote, this);
    }

    @Nonnull
    @Override
    protected EventHubsNamespace newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new EventHubsNamespace(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected EventHubNamespaces getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(EventHubsManager::namespaces).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Event Hubs Namespace";
    }
}
