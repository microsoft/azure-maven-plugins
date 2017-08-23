/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.function.bindings;

import com.microsoft.azure.serverless.functions.annotation.*;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BindingFactory {
    private static Map<Class<? extends Annotation>, Function<Annotation, BaseBinding>> map = new ConcurrentHashMap();

    static {
        map.put(BlobTrigger.class, a -> new BlobBinding((BlobTrigger) a));
        map.put(BlobInput.class, a -> new BlobBinding((BlobInput) a));
        map.put(BlobOutput.class, a -> new BlobBinding((BlobOutput) a));
        map.put(EventHubTrigger.class, a -> new EventHubBinding((EventHubTrigger) a));
        map.put(EventHubOutput.class, a -> new EventHubBinding((EventHubOutput) a));
        map.put(HttpTrigger.class, a -> new HttpBinding((HttpTrigger) a));
        map.put(HttpOutput.class, a -> new HttpBinding((HttpOutput) a));
        map.put(QueueTrigger.class, a -> new QueueBinding((QueueTrigger) a));
        map.put(QueueOutput.class, a -> new QueueBinding((QueueOutput) a));
        map.put(ServiceBusQueueTrigger.class, a -> new ServiceBusBinding((ServiceBusQueueTrigger) a));
        map.put(ServiceBusTopicTrigger.class, a -> new ServiceBusBinding((ServiceBusTopicTrigger) a));
        map.put(ServiceBusQueueOutput.class, a -> new ServiceBusBinding((ServiceBusQueueOutput) a));
        map.put(ServiceBusTopicOutput.class, a -> new ServiceBusBinding((ServiceBusTopicOutput) a));
        map.put(TableInput.class, a -> new TableBinding((TableInput) a));
        map.put(TableOutput.class, a -> new TableBinding((TableOutput) a));
        map.put(TimerTrigger.class, a -> new TimerBinding((TimerTrigger) a));
    }

    public static BaseBinding getBinding(final Annotation annotation) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        return map.containsKey(annotationType) ?
                map.get(annotationType).apply(annotation) :
                null;
    }
}
