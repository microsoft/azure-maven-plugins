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
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class EventHubsInstance extends AbstractAzResource<EventHubsInstance, EventHubsNamespace, EventHub> implements Deletable {
    @Nullable
    private EntityStatus status;
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

    public void activate() {
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

    public boolean sendMessage(String message) {
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
                return true;
            }
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
        return false;
    }

    public synchronized void startListening() {
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

    public synchronized void stopListening() {
        Optional.ofNullable(consumerAsyncClient).ifPresent(EventHubConsumerAsyncClient::close);
        Optional.ofNullable(messager).ifPresent(m -> m.info(AzureString.format("Stop listening to event hub ({0})\n", getName())));
        this.consumerAsyncClient = null;
        this.receivers.forEach(Disposable::dispose);
        this.receivers.clear();
    }

    public String getOrCreateListenConnectionString() {
        return getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN));
    }

    private void updateStatus(EntityStatus status) {
        final EventhubInner inner = remoteOptional().map(EventHub::innerModel).orElse(new EventhubInner());
        final EventHubsNamespace namespace = this.getParent();
        Optional.ofNullable(namespace.getParent().getRemote())
                .map(EventHubsManager::serviceClient)
                .map(EventHubManagementClient::getEventHubs)
                .ifPresent(c -> doModify(() -> c.createOrUpdate(getResourceGroupName(), namespace.getName(), getName(), inner.withStatus(status)), Status.UPDATING));
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
