/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.servicebus.model;

import com.azure.messaging.servicebus.*;
import com.azure.resourcemanager.servicebus.models.AccessRights;
import com.azure.resourcemanager.servicebus.models.EntityStatus;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.resource.message.ISenderReceiver;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class ServiceBusInstance<
        T extends ServiceBusInstance<T, P, F>, P, F>
        extends AbstractAzResource<T, ServiceBusNamespace, F> implements ISenderReceiver {
    @Nullable
    protected EntityStatus entityStatus;

    @Nullable
    protected ServiceBusProcessorClient processorClient;

    protected ServiceBusInstance(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, ServiceBusNamespace, F> module) {
        super(name, module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    public String getOrCreateListenConnectionString() {
        return getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN));
    }
    protected abstract String getOrCreateConnectionString(List<AccessRights> accessRights);

    @Nullable
    public EntityStatus getEntityStatus() {
        return this.entityStatus;
    }
    public abstract void updateStatus(EntityStatus status);

    @Override
    public abstract void sendMessage(String message);
    @Override
    public abstract void startReceivingMessage();
    @Override
    public boolean isListening() {
        return Objects.nonNull(this.processorClient);
    }

    @Override
    public boolean isSendEnabled() {
        return getFormalStatus().isRunning() && getEntityStatus() != EntityStatus.SEND_DISABLED;
    }

    @Override
    public synchronized void stopReceivingMessage() {
        Optional.ofNullable(processorClient).ifPresent(c -> {
            c.close();
            AzureMessager.getMessager().info(AzureString.format("Stop receiving message from Service Bus Queue ({0})\n", getName()));
        });
        this.processorClient = null;
    }
    protected void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        AzureMessager.getMessager().info(AzureString.format("Message received. Session: %s, Sequence #: %s. Contents: ", message.getMessageId(),
                message.getSequenceNumber()));
        AzureMessager.getMessager().debug(AzureString.format("\"%s\"\n",message.getBody()));
    }
    protected void processError(ServiceBusErrorContext context) {
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.error(AzureString.format("Error when receiving messages from Service Bus namespace: '%s'. Entity: '%s'\n",
                context.getFullyQualifiedNamespace(), context.getEntityPath()));
        if (!(context.getException() instanceof ServiceBusException)) {
            messager.error(AzureString.format("Non-ServiceBusException occurred: %s\n", context.getException()));
            stopReceivingMessage();
            return;
        }
        final ServiceBusException exception = (ServiceBusException) context.getException();
        final ServiceBusFailureReason reason = exception.getReason();
        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
                || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
                || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            messager.error(AzureString.format("An unrecoverable error occurred. Stopping processing with reason %s: %s\n",
                    reason, exception.getMessage()));
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            messager.error(AzureString.format("Message lock lost for message: %s\n", context.getException()));
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            messager.error(AzureString.format("Service is busy now, please try again later\n"));
        } else {
            messager.error(AzureString.format("Error source %s, reason %s, message: %s\n", context.getErrorSource(),
                    reason, context.getException()));
        }
        stopReceivingMessage();
    }

}
