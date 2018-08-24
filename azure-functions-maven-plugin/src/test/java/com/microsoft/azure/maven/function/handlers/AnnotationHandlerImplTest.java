/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpOutput;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.MobileTableInput;
import com.microsoft.azure.functions.annotation.MobileTableOutput;
import com.microsoft.azure.functions.annotation.NotificationHubOutput;
import com.microsoft.azure.functions.annotation.QueueOutput;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.azure.functions.annotation.SendGridOutput;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusTopicOutput;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.functions.annotation.TableInput;
import com.microsoft.azure.functions.annotation.TableOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.functions.annotation.TwilioSmsOutput;
import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class AnnotationHandlerImplTest {
    public static final String HTTP_TRIGGER_FUNCTION = "HttpTriggerFunction";
    public static final String HTTP_TRIGGER_METHOD = "httpTriggerMethod";
    public static final String QUEUE_TRIGGER_FUNCTION = "QueueTriggerFunction";
    public static final String QUEUE_TRIGGER_METHOD = "queueTriggerMethod";
    public static final String COSMOSDB_TRIGGER_FUNCTION = "cosmosDBTriggerFunction";
    public static final String COSMOSDB_TRIGGER_METHOD = "cosmosDBTriggerMethod";
    public static final String TIMER_TRIGGER_FUNCTION = "TimerTriggerFunction";
    public static final String TIMER_TRIGGER_METHOD = "timerTriggerMethod";
    public static final String MULTI_OUTPUT_FUNCTION = "MultiOutputFunction";
    public static final String MULTI_OUTPUT_METHOD = "multipleOutput";
    public static final String BLOB_TRIGGER_FUNCTION = "blobTriggerFunction";
    public static final String BLOB_TRIGGER_METHOD = "blobTriggerMethod";
    public static final String EVENTHUB_TRIGGER_FUNCTION = "eventHubTriggerFunction";
    public static final String EVENTHUB_TRIGGER_METHOD = "eventHubTriggerMethod";
    public static final String EVENTGRID_TRIGGER_FUNCTION = "eventGridTriggerFunction";
    public static final String EVENTGRID_TRIGGER_METHOD = "eventGridTriggerMethod";
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

        @FunctionName(COSMOSDB_TRIGGER_FUNCTION)
        public void cosmosDBTriggerMethod(@CosmosDBTrigger(name = "cosmos",
                                                databaseName = "db",
                                                collectionName = "cl",
                                                connectionStringSetting = "conn",
                                                leaseCollectionName = "lease") String in) {
        }

        @FunctionName(EVENTGRID_TRIGGER_FUNCTION)
        public void eventGridTriggerMethod(@EventGridTrigger(name = "eventgrid") String in) {
        }

        @FunctionName(TIMER_TRIGGER_FUNCTION)
        @CosmosDBOutput(name = "$return", databaseName = "db", collectionName = "col", connectionStringSetting = "conn")
        @MobileTableOutput(name = "$return", tableName = "table", connection = "conn", apiKey = "key")
        @NotificationHubOutput(name = "$return", hubName = "hub", connection = "conn")
        @SendGridOutput(name = "$return", apiKey = "key", to = "to", from = "from", subject = "sub", text = "text")
        @TwilioSmsOutput(name = "$return", accountSid = "sid", authToken = "auth", to = "to", from = "from", body = "b")
        public String timerTriggerMethod(@TimerTrigger(name = "timer", schedule = "", runOnStartup = false) String
                                                     timer,
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
    }

    @Test
    public void findFunctions() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();
        final Set<Method> functions = handler.findFunctions(Arrays.asList(getClassUrl()));

        assertEquals(10, functions.size());
        final List<String> methodNames = functions.stream().map(f -> f.getName()).collect(Collectors.toList());
        assertTrue(methodNames.contains(HTTP_TRIGGER_METHOD));
        assertTrue(methodNames.contains(QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(TIMER_TRIGGER_METHOD));
        assertTrue(methodNames.contains(MULTI_OUTPUT_METHOD));
        assertTrue(methodNames.contains(BLOB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(EVENTHUB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(SERVICE_BUS_QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(SERVICE_BUS_TOPIC_TRIGGER_METHOD));
        assertTrue(methodNames.contains(COSMOSDB_TRIGGER_METHOD));
        assertTrue(methodNames.contains(EVENTGRID_TRIGGER_METHOD));
    }

    @Test
    public void generateConfigurations() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();
        final Set<Method> functions = handler.findFunctions(Arrays.asList(getClassUrl()));
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(functions);
        configMap.values().forEach(config -> config.validate());

        assertEquals(10, configMap.size());

        verifyFunctionConfiguration(configMap, HTTP_TRIGGER_FUNCTION, HTTP_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, QUEUE_TRIGGER_FUNCTION, QUEUE_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, TIMER_TRIGGER_FUNCTION, TIMER_TRIGGER_METHOD, 8);

        verifyFunctionConfiguration(configMap, MULTI_OUTPUT_FUNCTION, MULTI_OUTPUT_METHOD, 3);

        verifyFunctionConfiguration(configMap, BLOB_TRIGGER_FUNCTION, BLOB_TRIGGER_METHOD, 5);

        verifyFunctionConfiguration(configMap, EVENTHUB_TRIGGER_FUNCTION, EVENTHUB_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, SERVICE_BUS_QUEUE_TRIGGER_FUNCTION, SERVICE_BUS_QUEUE_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, SERVICE_BUS_TOPIC_TRIGGER_FUNCTION, SERVICE_BUS_TOPIC_TRIGGER_METHOD, 2);

        verifyFunctionConfiguration(configMap, COSMOSDB_TRIGGER_FUNCTION, COSMOSDB_TRIGGER_METHOD, 1);

        verifyFunctionConfiguration(configMap, EVENTGRID_TRIGGER_FUNCTION, EVENTGRID_TRIGGER_METHOD, 1);
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
        assertEquals(bindingNum, functionConfig.getBindings().size());
    }
}
