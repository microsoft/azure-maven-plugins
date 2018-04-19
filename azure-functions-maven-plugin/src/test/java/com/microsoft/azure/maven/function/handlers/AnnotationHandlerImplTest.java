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
import java.util.Arrays;
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
    public static final String API_HUB_FILE_TRIGGER_FUNCTION = "apiHubFileTriggerFunction";
    public static final String API_HUB_FILE_TRIGGER_METHOD = "apiHubFileTriggerMethod";

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
        @CosmosDBOutput(name = "$return", databaseName = "db", collectionName = "col", connectionStringSetting = "conn")
        @MobileTableOutput(name = "$return", tableName = "table", connection = "conn", apiKey = "key")
        @NotificationHubOutput(name = "$return", hubName = "hub", connection = "conn")
        @SendGridOutput(name = "$return", apiKey = "key", to = "to", from = "from", subject = "sub", text = "text")
        @TwilioSmsOutput(name = "$return", accountSid = "sid", authToken = "auth", to = "to", from = "from", body = "b")
        public String timerTriggerMethod(@TimerTrigger(name = "timer", schedule = "") String timer,
                                         @CosmosDBOutput(name = "in1",
                                                 databaseName = "db",
                                                 collectionName = "col",
                                                 connectionStringSetting = "conn") String in1,
                                         @MobileTableInput(name = "in2",
                                                 tableName = "table",
                                                 id = "id",
                                                 connection = "conn",
                                                 apiKey = "key") String in2) {
            return "Hello!";
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

        @FunctionName(API_HUB_FILE_TRIGGER_FUNCTION)
        @ApiHubFileOutput(name = "$return", path = "", connection = "conn")
        public String apiHubFileTriggerMethod(@ApiHubFileTrigger(name = "in1", path = "p", connection = "c") String in1,
                                              @ApiHubFileInput(name = "in2", path = "p", connection = "c") String in2) {
            return "Hello!";
        }
    }

    @Test
    public void findFunctions() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();
        final Set<Method> functions = handler.findFunctions(Arrays.asList(getClassUrl()));

        assertEquals(9, functions.size());
        final List<String> methodNames = functions.stream().map(f -> f.getName()).collect(Collectors.toList());
        assertTrue(methodNames.contains(HTTP_TRIGGER_METHOD));
        assertTrue(methodNames.contains(QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(TIMER_TRIGGER_METHOD));
        assertTrue(methodNames.contains(MULTI_OUTPUT_METHOD));
        assertTrue(methodNames.contains(BLOB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(EVENTHUB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(SERVICE_BUS_QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(SERVICE_BUS_TOPIC_TRIGGER_METHOD));
        assertTrue(methodNames.contains(API_HUB_FILE_TRIGGER_METHOD));
    }

    @Test
    public void generateConfigurations() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();
        final Set<Method> functions = handler.findFunctions(Arrays.asList(getClassUrl()));
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(functions);
        configMap.values().forEach(config -> config.validate());

        assertEquals(9, configMap.size());

        verifyFunctionConfiguration(configMap, HTTP_TRIGGER_FUNCTION, HTTP_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, QUEUE_TRIGGER_FUNCTION, QUEUE_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, TIMER_TRIGGER_FUNCTION, TIMER_TRIGGER_METHOD, 8);

        verifyFunctionConfiguration(configMap, MULTI_OUTPUT_FUNCTION, MULTI_OUTPUT_METHOD, 3);

        verifyFunctionConfiguration(configMap, BLOB_TRIGGER_FUNCTION, BLOB_TRIGGER_METHOD, 5);

        verifyFunctionConfiguration(configMap, EVENTHUB_TRIGGER_FUNCTION, EVENTHUB_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, SERVICE_BUS_QUEUE_TRIGGER_FUNCTION, SERVICE_BUS_QUEUE_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, SERVICE_BUS_TOPIC_TRIGGER_FUNCTION, SERVICE_BUS_TOPIC_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, API_HUB_FILE_TRIGGER_FUNCTION, API_HUB_FILE_TRIGGER_METHOD, 3);
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

    private void verifyFunctionConfiguration(final Map<String, FunctionConfiguration> configMap,
                                             final String functionName, final String methodName, final int bindingNum) {
        assertTrue(configMap.containsKey(functionName));
        final FunctionConfiguration functionConfig = configMap.get(functionName);
        assertEquals(getFullyQualifiedMethodName(methodName), functionConfig.getEntryPoint());
        assertFalse(functionConfig.isDisabled());
        assertEquals(bindingNum, functionConfig.getBindings().size());
    }
}
