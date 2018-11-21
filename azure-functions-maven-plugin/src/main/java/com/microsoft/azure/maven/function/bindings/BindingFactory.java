/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.CosmosDBInput;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.HttpOutput;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueOutput;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.azure.functions.annotation.SendGridOutput;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusTopicOutput;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;
import com.microsoft.azure.functions.annotation.TableInput;
import com.microsoft.azure.functions.annotation.TableOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.functions.annotation.TwilioSmsOutput;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BindingFactory {
    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";
    private static Map<Class<? extends Annotation>, BindingEnum> bindingEnumMap = new ConcurrentHashMap<>();

    static {
        bindingEnumMap.put(BlobTrigger.class, BindingEnum.BLOB_TRIGGER);
        bindingEnumMap.put(BlobInput.class, BindingEnum.BLOB_INPUT);
        bindingEnumMap.put(BlobOutput.class, BindingEnum.BLOB_OUTPUT);
        bindingEnumMap.put(CosmosDBInput.class, BindingEnum.COSMOSDB_INPUT);
        bindingEnumMap.put(CosmosDBOutput.class, BindingEnum.COSMOSDB_OUTPUT);
        bindingEnumMap.put(CosmosDBTrigger.class, BindingEnum.COSMOSDB_TRIGGER);
        bindingEnumMap.put(EventHubTrigger.class, BindingEnum.EVENTHUB_TRIGGER);
        bindingEnumMap.put(EventHubOutput.class, BindingEnum.EVENTHUB_OUTPUT);
        bindingEnumMap.put(EventGridTrigger.class, BindingEnum.EVENTGRID_TRIGGER);
        bindingEnumMap.put(HttpTrigger.class, BindingEnum.HTTP_TRIGGER);
        bindingEnumMap.put(HttpOutput.class, BindingEnum.HTTP_OUTPUT);
        bindingEnumMap.put(QueueTrigger.class, BindingEnum.QUEUE_TRIGGER);
        bindingEnumMap.put(QueueOutput.class, BindingEnum.QUEUE_OUTPUT);
        bindingEnumMap.put(SendGridOutput.class, BindingEnum.SENDGRID_OUTPUT);
        bindingEnumMap.put(ServiceBusQueueTrigger.class, BindingEnum.SERVICEBUSQUEUE_TRIGGER);
        bindingEnumMap.put(ServiceBusTopicTrigger.class, BindingEnum.SERVICEBUSTOPIC_TRIGGER);
        bindingEnumMap.put(ServiceBusQueueOutput.class, BindingEnum.SERVICEBUSQUEUE_OUTPUT);
        bindingEnumMap.put(ServiceBusTopicOutput.class, BindingEnum.SERVICEBUSTOPIC_OUTPUT);
        bindingEnumMap.put(TableInput.class, BindingEnum.TABLE_INPUT);
        bindingEnumMap.put(TableOutput.class, BindingEnum.TABLE_OUTPUT);
        bindingEnumMap.put(TimerTrigger.class, BindingEnum.TIMER_TRIGGER);
        bindingEnumMap.put(TwilioSmsOutput.class, BindingEnum.TWILIOSMS_OUTPUT);
    }

    public static Binding getBinding(final Annotation annotation) {
        final BindingEnum bindingEnum = bindingEnumMap.get(annotation.annotationType());
        return bindingEnum == null ? null : new Binding(bindingEnum, annotation);
    }

    public static Binding getHTTPOutBinding(){
        final Binding result = new Binding(BindingEnum.HTTP_OUTPUT);
        result.setName(HTTP_OUTPUT_DEFAULT_NAME);
        return result;
    }
}
