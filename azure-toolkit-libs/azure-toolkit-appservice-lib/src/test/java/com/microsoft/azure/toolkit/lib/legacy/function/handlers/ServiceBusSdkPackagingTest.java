/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.storage.blob.BlobClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;
import com.microsoft.azure.functions.servicebus.ServiceBusMessageActions;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceBusSdkPackagingTest {
    public static class Functions {
        public void queue(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus")
                ServiceBusReceivedMessage message) {
        }

        public void topic(
                @ServiceBusTopicTrigger(name = "message", topicName = "topic", subscriptionName = "subscription",
                    connection = "ServiceBus", autoCompleteMessages = false)
                ServiceBusReceivedMessage message,
                @BindingName("message") ServiceBusMessageActions actions) {
        }

        public void queueBatch(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus",
                    cardinality = Cardinality.MANY, autoCompleteMessages = false)
                ServiceBusReceivedMessage[] messages,
                @BindingName("message") ServiceBusMessageActions actions) {
        }

        public void topicBatch(
                @ServiceBusTopicTrigger(name = "message", topicName = "topic", subscriptionName = "subscription",
                    connection = "ServiceBus", cardinality = Cardinality.MANY)
                List<ServiceBusReceivedMessage> messages) {
        }

        public void explicitAutoComplete(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus",
                    autoCompleteMessages = true)
                ServiceBusReceivedMessage message) {
        }

        public void stringMessage(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus",
                    autoCompleteMessages = false)
                String message) {
        }

        public void blobSdk(
                @BlobInput(name = "blob", path = "container/blob", connection = "Storage") BlobClient blob) {
        }

        public void blobString(
                @BlobInput(name = "blob", path = "container/blob", connection = "Storage") String blob) {
        }

        public void mixedBindings(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus")
                ServiceBusReceivedMessage message,
                @BlobInput(name = "blob", path = "container/blob", connection = "Storage") String blob,
                @ServiceBusQueueOutput(name = "output", queueName = "out", connection = "ServiceBus") String output) {
        }
    }

    @Test
    public void singleQueueMessageUsesExistingDeferredTrigger() throws Exception {
        final JsonNode binding = getOnlyBinding("queue", ServiceBusReceivedMessage.class);
        assertServiceBusTrigger(binding);
        assertEquals("queue", binding.path("queueName").asText());
        assertDeferred(binding);
        assertFalse(binding.has("autoCompleteMessages"));
    }

    @Test
    public void topicActionsDoNotCreateAnInputBinding() throws Exception {
        final JsonNode binding = getOnlyBinding("topic", ServiceBusReceivedMessage.class, ServiceBusMessageActions.class);
        assertServiceBusTrigger(binding);
        assertEquals("topic", binding.path("topicName").asText());
        assertEquals("subscription", binding.path("subscriptionName").asText());
        assertDeferred(binding);
        assertManualCompletion(binding);
    }

    @Test
    public void queueArrayUsesDeferredBatchTriggerWithoutActionsBinding() throws Exception {
        final JsonNode binding = getOnlyBinding("queueBatch", ServiceBusReceivedMessage[].class, ServiceBusMessageActions.class);
        assertServiceBusTrigger(binding);
        assertEquals("queue", binding.path("queueName").asText());
        assertEquals("MANY", binding.path("cardinality").asText());
        assertDeferred(binding);
        assertManualCompletion(binding);
    }

    @Test
    public void topicListUsesDeferredBatchTrigger() throws Exception {
        final JsonNode binding = getOnlyBinding("topicBatch", List.class);
        assertServiceBusTrigger(binding);
        assertEquals("topic", binding.path("topicName").asText());
        assertEquals("subscription", binding.path("subscriptionName").asText());
        assertEquals("MANY", binding.path("cardinality").asText());
        assertDeferred(binding);
        assertFalse(binding.has("autoCompleteMessages"));
    }

    @Test
    public void explicitDefaultInheritsHostAutoCompletion() throws Exception {
        final JsonNode binding = getOnlyBinding("explicitAutoComplete", ServiceBusReceivedMessage.class);
        assertDeferred(binding);
        assertFalse(binding.has("autoCompleteMessages"));
    }

    @Test
    public void stringTriggerRemainsNonDeferredAndPreservesBooleanFalse() throws Exception {
        final JsonNode binding = getOnlyBinding("stringMessage", String.class);
        assertServiceBusTrigger(binding);
        assertFalse(binding.has("properties"));
        assertManualCompletion(binding);
    }

    @Test
    public void blobSdkMetadataRemainsDeferred() throws Exception {
        final JsonNode binding = getOnlyBinding("blobSdk", BlobClient.class);
        assertBlobInput(binding);
        assertDeferred(binding);
    }

    @Test
    public void blobStringMetadataRemainsNonDeferred() throws Exception {
        final JsonNode binding = getOnlyBinding("blobString", String.class);
        assertBlobInput(binding);
        assertFalse(binding.has("properties"));
    }

    @Test
    public void sdkTriggerDoesNotChangeOtherInputOrOutputBindings() throws Exception {
        final JsonNode bindings = getBindings("mixedBindings", ServiceBusReceivedMessage.class, String.class, String.class);
        assertEquals(3, bindings.size());
        assertServiceBusTrigger(bindings.get(0));
        assertDeferred(bindings.get(0));
        assertBlobInput(bindings.get(1));
        assertFalse(bindings.get(1).has("properties"));
        final JsonNode output = bindings.get(2);
        assertEquals("serviceBus", output.path("type").asText());
        assertEquals("out", output.path("direction").asText());
        assertEquals("output", output.path("name").asText());
        assertEquals("out", output.path("queueName").asText());
        assertEquals("ServiceBus", output.path("connection").asText());
        assertFalse(output.has("properties"));
    }

    private JsonNode getOnlyBinding(final String methodName, final Class<?>... parameterTypes) throws Exception {
        final JsonNode bindings = getBindings(methodName, parameterTypes);
        assertEquals(1, bindings.size());
        return bindings.get(0);
    }

    private JsonNode getBindings(final String methodName, final Class<?>... parameterTypes) throws Exception {
        final FunctionConfiguration configuration = new AnnotationHandlerImpl()
            .generateConfiguration(Functions.class.getMethod(methodName, parameterTypes));
        configuration.validate();
        return new ObjectMapper().valueToTree(configuration).path("bindings");
    }

    private void assertBlobInput(final JsonNode binding) {
        assertEquals("blob", binding.path("type").asText());
        assertEquals("in", binding.path("direction").asText());
        assertEquals("blob", binding.path("name").asText());
        assertEquals("container/blob", binding.path("path").asText());
        assertEquals("Storage", binding.path("connection").asText());
    }

    private void assertServiceBusTrigger(final JsonNode binding) {
        assertEquals("serviceBusTrigger", binding.path("type").asText());
        assertEquals("in", binding.path("direction").asText());
        assertEquals("message", binding.path("name").asText());
        assertEquals("ServiceBus", binding.path("connection").asText());
    }

    private void assertDeferred(final JsonNode binding) {
        assertTrue(binding.path("properties").path("supportsDeferredBinding").isTextual());
        assertEquals("true", binding.path("properties").path("supportsDeferredBinding").asText());
    }

    private void assertManualCompletion(final JsonNode binding) {
        assertTrue(binding.path("autoCompleteMessages").isBoolean());
        assertFalse(binding.path("autoCompleteMessages").booleanValue());
    }
}
