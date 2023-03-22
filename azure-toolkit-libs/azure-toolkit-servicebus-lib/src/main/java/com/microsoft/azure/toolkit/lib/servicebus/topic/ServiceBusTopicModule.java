/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.servicebus.topic;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.servicebus.models.Topic;
import com.azure.resourcemanager.servicebus.models.Topics;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceBusTopicModule extends AbstractAzResourceModule<ServiceBusTopic, ServiceBusNamespace, Topic> {
    public static final String NAME = "topics";
    public ServiceBusTopicModule(@Nonnull ServiceBusNamespace parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    protected Topics getClient() {
        return Optional.ofNullable(this.parent.getRemote())
                .map(com.azure.resourcemanager.servicebus.models.ServiceBusNamespace::topics).orElse(null);
    }

    @Nullable
    @Override
    protected Topic loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final List<Topic> topicList = Optional.ofNullable(this.getClient())
                .map(topics -> topics.list().stream().collect(Collectors.toList())).orElse(Collections.emptyList());
        return topicList.stream().filter(topic -> name.equals(topic.name())).findAny().orElse(null);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Topic>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator())
                .orElse(Collections.emptyIterator());
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.parent.getRemote()).map(com.azure.resourcemanager.servicebus.models.ServiceBusNamespace::topics)
                .ifPresent(client -> client.deleteByName(this.getName()));
    }

    @Nonnull
    @Override
    protected ServiceBusTopic newResource(@Nonnull Topic remote) {
        return new ServiceBusTopic(remote, this);
    }

    @Nonnull
    @Override
    protected ServiceBusTopic newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ServiceBusTopic(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Service Bus Topic";
    }
}
