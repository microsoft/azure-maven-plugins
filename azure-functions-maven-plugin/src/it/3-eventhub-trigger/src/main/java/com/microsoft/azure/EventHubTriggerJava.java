package com.microsoft.azure;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Event Hub trigger.
 */
public class EventHubTriggerJava {
    /**
     * This function will be invoked when an event is received from Event Hub.
     */
    @FunctionName("EventHubTriggerJava")
    @QueueOutput(name = "$return", queueName = "out", connection = "AzureWebJobsDashboard")
    public String run(
        @EventHubTrigger(name = "message", eventHubName = "CIEventHub", connection = "CIEventHubConnection", consumerGroup = "$Default") String message,
        final ExecutionContext context
    ) {
        return message;
    }
}
