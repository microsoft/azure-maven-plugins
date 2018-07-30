/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

public class Function {
    @FunctionName("StorageQueueTriggerJava")
    @QueueOutput(name = "$return", queueName = "out", connection = "AzureWebJobsDashboard")
    public String queueHandler(
            @QueueTrigger(name = "in", queueName = "trigger", connection = "AzureWebJobsDashboard") String in,
            final ExecutionContext context
    ) {
        context.getLogger().info("Java Queue trigger function processed a message: " + in);
        return in;
    }
}
