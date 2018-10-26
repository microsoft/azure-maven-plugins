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
    public void run(
            @EventHubTrigger(name = "message", eventHubName = "trigger", connection = "CIEventHubConnection", consumerGroup = "$Default") String message,
            @EventHubOutput(name = "result", eventHubName = "output", connection = "CIEventHubConnection" ) OutputBinding<String> result,
            final ExecutionContext context
    ) {
        if(message.contains("CIInput")){
            result.setValue("CITest");
        }
    }
}
