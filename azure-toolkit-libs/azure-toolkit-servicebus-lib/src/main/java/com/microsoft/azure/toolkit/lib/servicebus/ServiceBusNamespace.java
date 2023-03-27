/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.servicebus.models.SkuTier;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.servicebus.queue.ServiceBusQueueModule;
import com.microsoft.azure.toolkit.lib.servicebus.topic.ServiceBusTopicModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ServiceBusNamespace extends AbstractAzResource<ServiceBusNamespace, ServiceBusNamespaceSubscription, com.azure.resourcemanager.servicebus.models.ServiceBusNamespace> implements Deletable {
    @Nonnull
    private final ServiceBusQueueModule queueModule;
    @Nonnull
    private final ServiceBusTopicModule topicModule;
    @Nullable
    private SkuTier skuTier;
    protected ServiceBusNamespace(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ServiceBusNamespaceModule module) {
        super(name, resourceGroupName, module);
        this.queueModule = new ServiceBusQueueModule(this);
        this.topicModule = new ServiceBusTopicModule(this);
    }

    protected ServiceBusNamespace(@Nonnull ServiceBusNamespace origin) {
        super(origin);
        this.queueModule = origin.queueModule;
        this.topicModule = origin.topicModule;
        this.skuTier = origin.skuTier;
    }

    protected ServiceBusNamespace(@Nonnull com.azure.resourcemanager.servicebus.models.ServiceBusNamespace remote, @Nonnull ServiceBusNamespaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.queueModule = new ServiceBusQueueModule(this);
        this.topicModule = new ServiceBusTopicModule(this);
        this.skuTier = remote.sku().tier();
    }

    @Override
    protected void updateAdditionalProperties(@Nullable com.azure.resourcemanager.servicebus.models.ServiceBusNamespace newRemote, @Nullable com.azure.resourcemanager.servicebus.models.ServiceBusNamespace oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        Optional.ofNullable(newRemote).ifPresent(r -> skuTier = r.sku().tier());
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        if (Objects.equals(skuTier, SkuTier.BASIC)) {
            return Collections.singletonList(queueModule);
        }
        return Arrays.asList(queueModule, topicModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.servicebus.models.ServiceBusNamespace remote) {
        return remote.innerModel().status();
    }
}
