/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.storage.blob.BlobClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServiceBusSdkPackagingTest {
    public static class Functions {
        public void queue(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus")
                ServiceBusReceivedMessage message) {
        }

        public void queueBatch(
                @ServiceBusQueueTrigger(name = "message", queueName = "queue", connection = "ServiceBus",
                    cardinality = Cardinality.MANY)
                ServiceBusReceivedMessage[] messages) {
        }

        public void topic(
                @ServiceBusTopicTrigger(name = "message", topicName = "topic", subscriptionName = "subscription",
                    connection = "ServiceBus")
                ServiceBusReceivedMessage message) {
        }

        public void blob(
                @BlobInput(name = "blob", path = "container/blob", connection = "Storage") BlobClient blob) {
        }
    }

    @Test
    public void queueMessageIsDeferred() throws Exception {
        final JsonNode binding = getOnlyBinding("queue", ServiceBusReceivedMessage.class);
        assertEquals("serviceBusTrigger", binding.path("type").asText());
        assertEquals("queue", binding.path("queueName").asText());
        assertDeferred(binding);
    }

    @Test
    public void queueBatchIsDeferred() throws Exception {
        final JsonNode binding = getOnlyBinding("queueBatch", ServiceBusReceivedMessage[].class);
        assertEquals("serviceBusTrigger", binding.path("type").asText());
        assertEquals("MANY", binding.path("cardinality").asText());
        assertDeferred(binding);
    }

    @Test
    public void topicMessageIsDeferred() throws Exception {
        final JsonNode binding = getOnlyBinding("topic", ServiceBusReceivedMessage.class);
        assertEquals("serviceBusTrigger", binding.path("type").asText());
        assertEquals("topic", binding.path("topicName").asText());
        assertEquals("subscription", binding.path("subscriptionName").asText());
        assertDeferred(binding);
    }

    @Test
    public void blobClientStaysDeferred() throws Exception {
        final JsonNode binding = getOnlyBinding("blob", BlobClient.class);
        assertEquals("blob", binding.path("type").asText());
        assertDeferred(binding);
    }

    private JsonNode getOnlyBinding(final String methodName, final Class<?>... parameterTypes) throws Exception {
        final FunctionConfiguration configuration = new AnnotationHandlerImpl()
            .generateConfiguration(Functions.class.getMethod(methodName, parameterTypes));
        final JsonNode bindings = new ObjectMapper().valueToTree(configuration).path("bindings");
        assertEquals(1, bindings.size());
        return bindings.get(0);
    }

    private void assertDeferred(final JsonNode binding) {
        final JsonNode flag = binding.path("properties").path("supportsDeferredBinding");
        assertTrue(flag.isTextual());
        assertEquals("true", flag.asText());
    }
}
