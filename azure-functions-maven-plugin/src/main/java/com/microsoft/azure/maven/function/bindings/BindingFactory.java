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
import com.microsoft.azure.functions.annotation.TableInput;
import com.microsoft.azure.functions.annotation.TableOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.functions.annotation.TwilioSmsOutput;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BindingFactory {
    private static Map<Class<? extends Annotation>, Class<? extends BaseBinding>> map = new ConcurrentHashMap();

    static {
        map.put(BlobTrigger.class, BlobBinding.class);
        map.put(BlobInput.class, BlobBinding.class);
        map.put(BlobOutput.class, BlobBinding.class);
        map.put(CosmosDBInput.class, CosmosDBBinding.class);
        map.put(CosmosDBOutput.class, CosmosDBBinding.class);
        map.put(CosmosDBTrigger.class, CosmosDBBinding.class);
        map.put(EventHubTrigger.class, EventHubBinding.class);
        map.put(EventHubOutput.class, EventHubBinding.class);
        map.put(EventGridTrigger.class, EventGridBinding.class);
        map.put(HttpTrigger.class, HttpBinding.class);
        map.put(HttpOutput.class, HttpBinding.class);
        map.put(MobileTableInput.class, MobileTableBinding.class);
        map.put(MobileTableOutput.class, MobileTableBinding.class);
        map.put(NotificationHubOutput.class, NotificationHubBinding.class);
        map.put(QueueTrigger.class, QueueBinding.class);
        map.put(QueueOutput.class, QueueBinding.class);
        map.put(SendGridOutput.class, SendGridBinding.class);
        map.put(ServiceBusQueueTrigger.class, ServiceBusBinding.class);
        map.put(ServiceBusTopicTrigger.class, ServiceBusBinding.class);
        map.put(ServiceBusQueueOutput.class, ServiceBusBinding.class);
        map.put(ServiceBusTopicOutput.class, ServiceBusBinding.class);
        map.put(TableInput.class, TableBinding.class);
        map.put(TableOutput.class, TableBinding.class);
        map.put(TimerTrigger.class, TimerBinding.class);
        map.put(TwilioSmsOutput.class, TwilioBinding.class);
    }

    public static BaseBinding getBinding(final Annotation annotation) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        return map.containsKey(annotationType) ?
                createNewInstance(map.get(annotationType), annotation) :
                null;
    }

    private static BaseBinding createNewInstance(final Class<? extends BaseBinding> binding,
                                                 final Annotation annotation) {
        try {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final Constructor constructor = binding.getConstructor(annotationType);
            return (BaseBinding) constructor.newInstance(annotation);
        } catch (Exception e) {
            // Swallow it
        }
        return null;
    }
}
