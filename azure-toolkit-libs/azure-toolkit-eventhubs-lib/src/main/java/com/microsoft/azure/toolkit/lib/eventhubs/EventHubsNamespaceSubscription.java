/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.resourcemanager.eventhubs.EventHubsManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class EventHubsNamespaceSubscription extends AbstractAzServiceSubscription<EventHubsNamespaceSubscription, EventHubsManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final EventHubsNamespaceModule eventHubsNamespaceModule;


    protected EventHubsNamespaceSubscription(@Nonnull String subscriptionId, @Nonnull AzureEventHubsNamespace service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.eventHubsNamespaceModule = new EventHubsNamespaceModule(this);
    }

    protected EventHubsNamespaceSubscription(@Nonnull EventHubsManager manager, @Nonnull AzureEventHubsNamespace service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    public EventHubsNamespaceModule eventHubsNamespaces() {
        return this.eventHubsNamespaceModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(eventHubsNamespaceModule);
    }

    @Override
    public List<Region> listSupportedRegions(@NotNull String resourceType) {
        return super.listSupportedRegions(this.eventHubsNamespaceModule.getName());
    }
}
