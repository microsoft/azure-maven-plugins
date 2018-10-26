/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.invoker.storage;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;

import java.util.ArrayList;
import java.util.List;

public class EventProcessor implements IEventProcessor {
    private static List<String> messages = new ArrayList<>();

    // OnOpen is called when a new event processor instance is created by the host.
    @Override
    public void onOpen(PartitionContext context) throws Exception {
        System.out.println("SAMPLE: Partition " + context.getPartitionId() + " is opening");
    }

    // OnClose is called when an event processor instance is being shut down.
    @Override
    public void onClose(PartitionContext context, CloseReason reason) throws Exception {
        System.out.println("SAMPLE: Partition " + context.getPartitionId() + " is closing for " + reason.toString());
    }

    // onError is called when an error occurs in EventProcessorHost code that is tied to this partition.
    @Override
    public void onError(PartitionContext context, Throwable error) {
        System.out.println("SAMPLE: Partition " + context.getPartitionId() + " onError: " + error.toString());
    }

    // onEvents is called when events are received on this partition of the Event Hub.
    @Override
    public void onEvents(PartitionContext context, Iterable<EventData> events) throws Exception {
        System.out.println("SAMPLE: Partition " + context.getPartitionId() + " got event batch");
        try {
            events.forEach(eventData -> messages.add(new String(eventData.getBytes())));
        } catch (Exception e) {
            System.out.println("Processing failed for an event: " + e.toString());
        }
    }

    public static List<String> getMessages() {
        final List<String> result = new ArrayList<>();
        result.addAll(messages);
        messages.clear();
        return result;
    }

}

