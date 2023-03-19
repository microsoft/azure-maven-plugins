/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.resourcemanager.servicebus.ServiceBusManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class ServiceBusNamespaceSubscription extends AbstractAzServiceSubscription<ServiceBusNamespaceSubscription, ServiceBusManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final ServiceBusNamespaceModule serviceBusNamespaceModule;

    protected ServiceBusNamespaceSubscription(@Nonnull String subscriptionId, @Nonnull AzureServiceBusNamespace service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.serviceBusNamespaceModule = new ServiceBusNamespaceModule(this);
    }

    protected ServiceBusNamespaceSubscription(@Nonnull ServiceBusManager manager, @Nonnull AzureServiceBusNamespace service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Override
    public List<Region> listSupportedRegions(@Nonnull String resourceType) {
        return super.listSupportedRegions(this.serviceBusNamespaceModule.getName());
    }
}
