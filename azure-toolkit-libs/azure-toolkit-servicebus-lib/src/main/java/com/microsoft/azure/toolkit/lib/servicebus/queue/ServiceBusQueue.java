/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.servicebus.queue;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.resourcemanager.servicebus.ServiceBusManager;
import com.azure.resourcemanager.servicebus.fluent.ServiceBusManagementClient;
import com.azure.resourcemanager.servicebus.fluent.models.SBAuthorizationRuleInner;
import com.azure.resourcemanager.servicebus.fluent.models.SBQueueInner;
import com.azure.resourcemanager.servicebus.models.AccessRights;
import com.azure.resourcemanager.servicebus.models.EntityStatus;
import com.azure.resourcemanager.servicebus.models.Queue;
import com.azure.resourcemanager.servicebus.models.QueueAuthorizationRule;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;
import com.microsoft.azure.toolkit.lib.servicebus.model.ServiceBusInstance;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceBusQueue extends ServiceBusInstance<ServiceBusQueue, ServiceBusNamespace, Queue> implements Deletable {
    protected ServiceBusQueue(@Nonnull String name, @Nonnull ServiceBusQueueModule module) {
        super(name, module);
    }

    protected ServiceBusQueue(@Nonnull Queue remote, @Nonnull ServiceBusQueueModule module) {
        super(remote.name(), module);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable Queue newRemote, @Nullable Queue oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.entityStatus = Optional.ofNullable(newRemote).map(Queue::innerModel).map(SBQueueInner::status).orElse(null);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Queue remote) {
        return remote.innerModel().status().toString();
    }

    @Override
    public void updateStatus(EntityStatus status) {
        final SBQueueInner inner = remoteOptional().map(Queue::innerModel).orElse(new SBQueueInner());
        final ServiceBusNamespace namespace = this.getParent();
        Optional.ofNullable(namespace.getParent().getRemote())
                .map(ServiceBusManager::serviceClient)
                .map(ServiceBusManagementClient::getQueues)
                .ifPresent(c -> doModify(() -> c.createOrUpdate(getResourceGroupName(), namespace.getName(), getName(), inner.withStatus(status)), Status.UPDATING));
    }

    @Override
    public void sendMessage(String message) {
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Sending message to Service Bus Queue (%s)...\n", getName()));
        try (final ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(getOrCreateConnectionString(Collections.singletonList(AccessRights.SEND)))
                .sender()
                .queueName(getName())
                .buildClient()) {
            senderClient.sendMessage(new ServiceBusMessage(message));
            messager.info("Successfully send message ");
            messager.debug(AzureString.format("\"%s\"", message));
            messager.info(AzureString.format(" to Service Bus Queue (%s)\n", getName()));
        } catch (final Exception e) {
            messager.error(AzureString.format("Failed to send message to Service Bus Queue (%s): %s", getName(), e));
        }
    }

    @Override
    public synchronized void startReceivingMessage() {
        messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start receiving message from Service Bus Queue ({0})\n", getName()));
        this.processorClient = new ServiceBusClientBuilder()
                .connectionString(getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN)))
                .processor()
                .queueName(getName())
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .processMessage(this::processMessage)
                .processError(this::processError)
                .disableAutoComplete()  // Complete - causes the message to be deleted from the queue or topic.
                .buildProcessorClient();
        processorClient.start();
    }

    @Override
    protected String getOrCreateConnectionString(List<AccessRights> accessRights) {
        final List<QueueAuthorizationRule> connectionStrings = Optional.ofNullable(getRemote())
                .map(queue -> queue.authorizationRules().list().stream()
                        .filter(rule -> new HashSet<>(rule.rights()).containsAll(accessRights))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        if (connectionStrings.size() > 0) {
            return connectionStrings.get(0).getKeys().primaryConnectionString();
        }
        final ServiceBusManager manager = getParent().getParent().getRemote();
        if (Objects.isNull(manager)) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final String accessRightsStr = StringUtils.join(accessRights, "-");
        manager.serviceClient().getQueues().createOrUpdateAuthorizationRule(getResourceGroupName(), getParent().getName(),
                getName(), accessRightsStr, new SBAuthorizationRuleInner().withRights(accessRights));
        return manager.serviceClient().getQueues().listKeys(getResourceGroupName(), getParent().getName(),
                getName(), accessRightsStr).primaryConnectionString();
    }
}
