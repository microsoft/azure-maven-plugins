package com.microsoft.azure;

import java.util.*;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke
     * it using "curl" command in bash: 1. curl -d "HTTP Body" {your
     * host}/api/HttpTrigger-Java&code={your function key} 2. curl "{your
     * host}/api/HttpTrigger-Java?name=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, to invoke HttpTrigger
     * deployed to Azure, see
     * here(https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook#authorization-keys)
     * on how to get function key for your app.
     */
    @FunctionName("HttpTrigger-Java")
    public void run(@HttpTrigger(name = "req", methods = {HttpMethod.GET,
            HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                    @EventHubOutput(name = "output", eventHubName = "CIEventHub", connection = "CIEventHubConnection") OutputBinding<String> output,
                    final ExecutionContext context) {
        output.setValue("CITrigger");
    }
}
