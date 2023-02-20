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
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class EventHubsInstance extends AbstractAzResource<EventHubsInstance, EventHubsNamespace, EventHub> implements Deletable {
    @Nullable
    private EntityStatus status;
    @Nullable
    private EventHubConsumerAsyncClient consumerAsyncClient;
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
        this.status = Optional.ofNullable(newRemote).map(EventHub::innerModel).map(EventhubInner::status).orElse(null);
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

    public boolean isActive() {
        return Objects.equals(this.status, EntityStatus.ACTIVE);
    }

    public boolean isDisabled() {
        return Objects.equals(this.status, EntityStatus.DISABLED);
    }

    public boolean isSendDisabled() {
        return Objects.equals(this.status, EntityStatus.SEND_DISABLED);
    }

    public void active() {
        updateStatus(EntityStatus.ACTIVE);
    }

    public void disable() {
        updateStatus(EntityStatus.DISABLED);
    }

    public void disableSending() {
        updateStatus(EntityStatus.SEND_DISABLED);
    }

    public boolean isListening() {
        return Objects.nonNull(this.consumerAsyncClient);
    }

    public void publishEvents(String message) {
        final IAzureMessager messager = AzureMessager.getMessager();
        try (final EventHubProducerClient producer = new EventHubClientBuilder()
                .connectionString(getOrCreateConnectionString(Collections.singletonList(AccessRights.SEND)))
                .buildProducerClient()) {
            EventDataBatch eventDataBatch = producer.createBatch();
            final EventData eventData =  new EventData(message);
            if (!eventDataBatch.tryAdd(eventData)) {
                producer.send(eventDataBatch);
                eventDataBatch = producer.createBatch();
                if (!eventDataBatch.tryAdd(eventData)) {
                    throw new AzureToolkitRuntimeException("Event is too large for an empty batch. Max size: "
                            + eventDataBatch.getMaxSizeInBytes());
                }
            }
            if (eventDataBatch.getCount() > 0) {
                producer.send(eventDataBatch);
                messager.info(String.format("Successfully send message to event hub %s", getName()));
            }
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    public void startListening() {
        messager = AzureMessager.getMessager();
        messager.info(String.format("Start listening to event hub %s ...", getName()));
        remoteOptional().ifPresent(remote -> remote.listConsumerGroups().forEach(eventHubConsumerGroup ->
                remote.partitionIds().forEach(partitionId -> {
                    this.consumerAsyncClient = new EventHubClientBuilder()
                        .connectionString(getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN)))
                        .consumerGroup(eventHubConsumerGroup.name())
                        .buildAsyncConsumerClient();
                    messager.info(String.format("Created receiver for partition %s and consumerGroup %s", partitionId, eventHubConsumerGroup.name()));
                    Optional.ofNullable(this.consumerAsyncClient).ifPresent(client -> client.receiveFromPartition(partitionId, EventPosition.latest())
                            .subscribe(partitionEvent -> messager.info(String.format("Message Received from partition %s and consumerGroup %s: %s",
                                    partitionId, eventHubConsumerGroup.name(), partitionEvent.getData().getBodyAsString()))));
                })));
    }

    public void stopListening() {
        if (Objects.isNull(this.consumerAsyncClient)) {
            return;
        }
        this.consumerAsyncClient.close();
        this.consumerAsyncClient = null;
        Optional.ofNullable(messager).ifPresent(m ->
                m.info(String.format("Stop listening to event hub %s ", getName())));
    }

    private void updateStatus(EntityStatus status) {
        final EventhubInner inner = remoteOptional().map(EventHub::innerModel).orElse(new EventhubInner());
        final EventHubsNamespace namespace = this.getParent();
        Optional.ofNullable(namespace.getParent().getRemote())
                .map(EventHubsManager::serviceClient)
                .map(EventHubManagementClient::getEventHubs)
                .ifPresent(c -> doModify(() -> c.createOrUpdate(getResourceGroupName(), namespace.getName(), getName(), inner.withStatus(status)), Status.UPDATING));
    }

    public String getOrCreateConnectionString(List<AccessRights> accessRights) {
        final List<EventHubAuthorizationRule> connectionStrings = Optional.ofNullable(getRemote())
                .map(eventHubInstance -> eventHubInstance.listAuthorizationRules().stream()
                        .filter(rule -> new HashSet<>(rule.rights()).containsAll(accessRights))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        final EventHubsManager manager = getParent().getParent().getRemote();
        if (connectionStrings.size() > 0) {
            return connectionStrings.get(0).getKeys().primaryConnectionString();
        }
        if (Objects.isNull(manager)) {
            return null;
        }
        final String accessRightsStr = StringUtils.join(accessRights, ",");
        return manager.eventHubAuthorizationRules().define(String.format("policy-%s-%s", accessRightsStr, Utils.getTimestamp()))
                .withExistingEventHub(getResourceGroupName(), getParent().getName(), getName())
                .withSendAndListenAccess()
                .create().getKeys().primaryConnectionString();
    }
}
