/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;
import com.microsoft.azure.serverless.functions.annotation.*;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AnnotationHandlerImplTest {
    public static final String HTTP_TRIGGER_FUNCTION = "HttpTriggerFunction";
    public static final String HTTP_TRIGGER_METHOD = "httpTriggerMethod";
    public static final String QUEUE_TRIGGER_FUNCTION = "QueueTriggerFunction";
    public static final String QUEUE_TRIGGER_METHOD = "queueTriggerMethod";
    public static final String TIMER_TRIGGER_FUNCTION = "TimerTriggerFunction";
    public static final String TIMER_TRIGGER_METHOD = "timerTriggerMethod";
    public static final String MULTI_OUTPUT_FUNCTION = "MultiOutputFunction";
    public static final String MULTI_OUTPUT_METHOD = "multipleOutput";
    public static final String BLOB_TRIGGER_FUNCTION = "blobTriggerFunction";
    public static final String BLOB_TRIGGER_METHOD = "blobTriggerMethod";
    public static final String EVENTHUB_TRIGGER_FUNCTION = "eventHubTriggerFunction";
    public static final String EVENTHUB_TRIGGER_METHOD = "eventHubTriggerMethod";
    public static final String SERVICE_BUS_QUEUE_TRIGGER_FUNCTION = "serviceBusQueueTriggerFunction";
    public static final String SERVICE_BUS_QUEUE_TRIGGER_METHOD = "serviceBusQueueTriggerMethod";
    public static final String SERVICE_BUS_TOPIC_TRIGGER_FUNCTION = "serviceBusTopicTriggerFunction";
    public static final String SERVICE_BUS_TOPIC_TRIGGER_METHOD = "serviceBusTopicTriggerMethod";

    public class FunctionEntryPoints {
        @FunctionName(HTTP_TRIGGER_FUNCTION)
        public String httpTriggerMethod(@HttpTrigger(name = "req") String req) {
            return "Hello!";
        }

        @FunctionName(MULTI_OUTPUT_FUNCTION)
        @HttpOutput(name = "$return")
        @QueueOutput(name = "$return", queueName = "qOut", connection = "conn")
        public String multipleOutput(@HttpTrigger(name = "req") String req) {
            return "Hello!";
        }

        @FunctionName(QUEUE_TRIGGER_FUNCTION)
        public void queueTriggerMethod(@QueueTrigger(name = "in", queueName = "qIn", connection = "conn") String in,
                                       @QueueOutput(name = "out", queueName = "qOut", connection = "conn") String out) {
        }

        @FunctionName(TIMER_TRIGGER_FUNCTION)
        public void timerTriggerMethod(@TimerTrigger(name = "timer", schedule = "") String timer) {
        }

        @FunctionName(BLOB_TRIGGER_FUNCTION)
        @StorageAccount("storageAccount")
        @BlobOutput(name = "$return", path = "path")
        @TableOutput(name = "$return", tableName = "table")
        public String blobTriggerMethod(@BlobTrigger(name = "in1", path = "path") String in1,
                                        @BlobInput(name = "in2", path = "path") String in2,
                                        @TableInput(name = "in3", tableName = "table") String in3) {
            return "Hello!";
        }

        @FunctionName(EVENTHUB_TRIGGER_FUNCTION)
        @EventHubOutput(name = "$return", eventHubName = "eventHub", connection = "conn")
        public String eventHubTriggerMethod(
                @EventHubTrigger(name = "in", eventHubName = "eventHub", connection = "conn") String in) {
            return "Hello!";
        }

        @FunctionName(SERVICE_BUS_QUEUE_TRIGGER_FUNCTION)
        @ServiceBusQueueOutput(name = "$return", queueName = "queue", connection = "conn")
        public String serviceBusQueueTriggerMethod(
                @ServiceBusQueueTrigger(name = "in", queueName = "queue", connection = "conn") String in) {
            return "Hello!";
        }

        @FunctionName(SERVICE_BUS_TOPIC_TRIGGER_FUNCTION)
        @ServiceBusTopicOutput(name = "$return", topicName = "topic", subscriptionName = "subs", connection = "conn")
        public String serviceBusTopicTriggerMethod(@ServiceBusTopicTrigger(name = "in", topicName = "topic",
                subscriptionName = "subs", connection = "conn") String in) {
            return "Hello!";
        }
    }

    @Test
    public void findFunctions() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();
        final Set<Method> functions = handler.findFunctions(getClassUrl());

        assertEquals(8, functions.size());
        final List<String> methodNames = functions.stream().map(f -> f.getName()).collect(Collectors.toList());
        assertTrue(methodNames.contains(HTTP_TRIGGER_METHOD));
        assertTrue(methodNames.contains(QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(TIMER_TRIGGER_METHOD));
        assertTrue(methodNames.contains(MULTI_OUTPUT_METHOD));
        assertTrue(methodNames.contains(BLOB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(EVENTHUB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(SERVICE_BUS_QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(SERVICE_BUS_TOPIC_TRIGGER_METHOD));
    }

    @Test
    public void generateConfigurations() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();
        final Set<Method> functions = handler.findFunctions(getClassUrl());
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(functions);
        configMap.values().forEach(config -> config.validate());

        assertEquals(8, configMap.size());

        assertTrue(configMap.containsKey(HTTP_TRIGGER_FUNCTION));
        final FunctionConfiguration httpTriggerFunctionConfig = configMap.get(HTTP_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(HTTP_TRIGGER_METHOD), httpTriggerFunctionConfig.getEntryPoint());
        assertFalse(httpTriggerFunctionConfig.isDisabled());
        assertEquals(2, httpTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(QUEUE_TRIGGER_FUNCTION));
        final FunctionConfiguration queueTriggerFunctionConfig = configMap.get(QUEUE_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(QUEUE_TRIGGER_METHOD), queueTriggerFunctionConfig.getEntryPoint());
        assertEquals(2, queueTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(TIMER_TRIGGER_FUNCTION));
        final FunctionConfiguration timerTriggerFunctionConfig = configMap.get(TIMER_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(TIMER_TRIGGER_METHOD), timerTriggerFunctionConfig.getEntryPoint());
        assertEquals(1, timerTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(MULTI_OUTPUT_FUNCTION));
        final FunctionConfiguration multiOutputFunctionConfig = configMap.get(MULTI_OUTPUT_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(MULTI_OUTPUT_METHOD), multiOutputFunctionConfig.getEntryPoint());
        assertFalse(multiOutputFunctionConfig.isDisabled());
        assertEquals(3, multiOutputFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(BLOB_TRIGGER_FUNCTION));
        final FunctionConfiguration blobTriggerFunctionConfig = configMap.get(BLOB_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(BLOB_TRIGGER_METHOD), blobTriggerFunctionConfig.getEntryPoint());
        assertFalse(blobTriggerFunctionConfig.isDisabled());
        assertEquals(5, blobTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(EVENTHUB_TRIGGER_FUNCTION));
        final FunctionConfiguration eventHubTriggerFunctionConfig = configMap.get(EVENTHUB_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(EVENTHUB_TRIGGER_METHOD),
                eventHubTriggerFunctionConfig.getEntryPoint());
        assertFalse(eventHubTriggerFunctionConfig.isDisabled());
        assertEquals(2, eventHubTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(SERVICE_BUS_QUEUE_TRIGGER_FUNCTION));
        final FunctionConfiguration sbQueueTriggerFunctionConfig = configMap.get(SERVICE_BUS_QUEUE_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(SERVICE_BUS_QUEUE_TRIGGER_METHOD),
                sbQueueTriggerFunctionConfig.getEntryPoint());
        assertFalse(sbQueueTriggerFunctionConfig.isDisabled());
        assertEquals(2, sbQueueTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(SERVICE_BUS_TOPIC_TRIGGER_FUNCTION));
        final FunctionConfiguration sbTopicTriggerFunctionConfig = configMap.get(SERVICE_BUS_TOPIC_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(SERVICE_BUS_TOPIC_TRIGGER_METHOD),
                sbTopicTriggerFunctionConfig.getEntryPoint());
        assertFalse(sbTopicTriggerFunctionConfig.isDisabled());
        assertEquals(2, sbTopicTriggerFunctionConfig.getBindings().size());
    }

    private AnnotationHandlerImpl getAnnotationHandler() {
        final Log log = mock(Log.class);
        return new AnnotationHandlerImpl(log);
    }

    private URL getClassUrl() {
        return ClasspathHelper.forPackage("com.microsoft.azure.maven.function.handlers")
                .iterator()
                .next();
    }

    private String getFullyQualifiedMethodName(final String methodName) {
        return FunctionEntryPoints.class.getCanonicalName() + "." + methodName;
    }
}
