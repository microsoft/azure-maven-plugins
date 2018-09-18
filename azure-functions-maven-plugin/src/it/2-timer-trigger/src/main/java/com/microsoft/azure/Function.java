/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import java.time.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

public class Function {
    @FunctionName("TimerTriggerJava")
    @QueueOutput(name = "$return", queueName = "out", connection = "AzureWebJobsDashboard")
    public String run(
            @TimerTrigger(name = "timerInfo", schedule = "*/1 * * * * *") String timerInfo,
            final ExecutionContext context
    ) {
        context.getLogger().info("Java Timer trigger function executed at: " + LocalDateTime.now());
        return "successfully triggered";
    }
}
