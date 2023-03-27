/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.messaging.eventhubs.*;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.azure.resourcemanager.eventhubs.EventHubsManager;
import com.azure.resourcemanager.eventhubs.fluent.EventHubManagementClient;
import com.azure.resourcemanager.eventhubs.fluent.models.EventhubInner;
import com.azure.resourcemanager.eventhubs.models.AccessRights;
import com.azure.resourcemanager.eventhubs.models.EntityStatus;
import com.azure.resourcemanager.eventhubs.models.EventHub;
import com.azure.resourcemanager.eventhubs.models.EventHubAuthorizationRule;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.message.ISenderReceiver;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class EventHubsInstance extends AbstractAzResource<EventHubsInstance, EventHubsNamespace, EventHub> implements Deletable, ISenderReceiver {
    @Nullable
    @Getter
    private EntityStatus entityStatus;
    @Nullable
    private EventHubConsumerAsyncClient consumerAsyncClient;
    private final List<Disposable> receivers = new ArrayList<>();
    @Nullable
    private IAzureMessager messager;
    protected EventHubsInstance(@Nonnull String name, @Nonnull EventHubsInstanceModule module) {
        super(name, module);
    }

    protected EventHubsInstance(@Nonnull EventHub remote, @Nonnull EventHubsInstanceModule module) {
        super(remote.name(), module);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable EventHub newRemote, @Nullable EventHub oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.entityStatus = Optional.ofNullable(newRemote).map(EventHub::innerModel).map(EventhubInner::status).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull EventHub remote) {
        return remote.innerModel().status().toString();
    }

    public void updateStatus(EntityStatus status) {
        final EventhubInner inner = remoteOptional().map(EventHub::innerModel).orElse(new EventhubInner());
        final EventHubsNamespace namespace = this.getParent();
        Optional.ofNullable(namespace.getParent().getRemote())
                .map(EventHubsManager::serviceClient)
                .map(EventHubManagementClient::getEventHubs)
                .ifPresent(c -> doModify(() -> c.createOrUpdate(getResourceGroupName(), namespace.getName(), getName(), inner.withStatus(status)), Status.UPDATING));
    }

    @Override
    public synchronized void startReceivingMessage() {
        final AzureConfiguration config = Azure.az().config();
        final String consumerGroupName = config.getEventHubsConsumerGroup();
        messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start listening to event hub ({0}) for consumerGroup ({1})...\n", getName(), consumerGroupName));
        messager.info("You can change default consumer group in Azure Settings\n");
        remoteOptional().ifPresent(remote -> remote.partitionIds().forEach(partitionId -> {
            this.consumerAsyncClient = new EventHubClientBuilder()
                    .connectionString(getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN)))
                    .consumerGroup(consumerGroupName)
                    .buildAsyncConsumerClient();
            messager.info(AzureString.format("Created receiver for partition ({0})\n", partitionId));
            Optional.ofNullable(this.consumerAsyncClient).ifPresent(client ->
                    receivers.add(client.receiveFromPartition(partitionId, EventPosition.latest())
                            .subscribe(partitionEvent -> {
                                messager.info(AzureString.format("Message Received from partition (%s): ", partitionId));
                                messager.debug(AzureString.format("\"%s\"\n", partitionEvent.getData().getBodyAsString()));
                            })));
        }));
    }

    @Override
    public synchronized void stopReceivingMessage() {
        Optional.ofNullable(consumerAsyncClient).ifPresent(EventHubConsumerAsyncClient::close);
        Optional.ofNullable(messager).orElse(AzureMessager.getMessager()).info(AzureString.format("Stop listening to event hub ({0})\n", getName()));
        this.consumerAsyncClient = null;
        this.receivers.forEach(Disposable::dispose);
        this.receivers.clear();
    }

    @Override
    public boolean isListening() {
        return Objects.nonNull(this.consumerAsyncClient);
    }

    @Override
    public boolean isSendEnabled() {
        return getFormalStatus().isRunning() && getEntityStatus() != EntityStatus.SEND_DISABLED;
    }

    @Override
    public void sendMessage(String message) {
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Sending message to Event Hub (%s)...\n", getName()));
        try (final EventHubProducerClient producer = new EventHubClientBuilder()
                .connectionString(getOrCreateConnectionString(Collections.singletonList(AccessRights.SEND)))
                .buildProducerClient()) {
            EventDataBatch eventDataBatch = producer.createBatch();
            final EventData eventData =  new EventData(message);
            if (!eventDataBatch.tryAdd(eventData)) {
                producer.send(eventDataBatch);
                eventDataBatch = producer.createBatch();
                if (!eventDataBatch.tryAdd(eventData)) {
                    final String reason = "Event is too large for an empty batch. Max size: "
                            + eventDataBatch.getMaxSizeInBytes();
                    messager.error(AzureString.format("Failed to send message to Event Hub (%s): %s", getName(), reason));
                }
            }
            if (eventDataBatch.getCount() > 0) {
                producer.send(eventDataBatch);
                messager.info("Successfully sent message ");
                messager.success(AzureString.format("\"%s\"", message));
                messager.info(AzureString.format(" to Event Hub (%s)\n", getName()));
            }
        } catch (final Exception e) {
            messager.error(AzureString.format("Failed to send message to Event Hub (%s): %s", getName(), e));
        }
    }

    public String getOrCreateListenConnectionString() {
        return getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN));
    }

    private String getOrCreateConnectionString(List<AccessRights> accessRights) {
        final List<EventHubAuthorizationRule> connectionStrings = Optional.ofNullable(getRemote())
                .map(eventHubInstance -> eventHubInstance.listAuthorizationRules().stream()
                        .filter(rule -> new HashSet<>(rule.rights()).containsAll(accessRights))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        if (connectionStrings.size() > 0) {
            return connectionStrings.get(0).getKeys().primaryConnectionString();
        }
        final EventHubsManager manager = getParent().getParent().getRemote();
        if (Objects.isNull(manager)) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final String accessRightsStr = StringUtils.join(accessRights, "-");
        final EventHubAuthorizationRule.DefinitionStages.WithAccessPolicy policy = manager.eventHubAuthorizationRules()
                .define(String.format("policy-%s-%s", accessRightsStr, Utils.getTimestamp()))
                .withExistingEventHub(getResourceGroupName(), getParent().getName(), getName());
        EventHubAuthorizationRule.DefinitionStages.WithCreate withCreate = policy.withListenAccess();
        if (accessRights.contains(AccessRights.MANAGE)) {
            withCreate = policy.withManageAccess();
        } else if (accessRights.contains(AccessRights.SEND) && accessRights.contains(AccessRights.LISTEN)) {
            withCreate = policy.withSendAndListenAccess();
        } else if (accessRights.contains(AccessRights.SEND)) {
            withCreate = policy.withSendAccess();
        }
        return withCreate.create().getKeys().primaryConnectionString();
    }
}
