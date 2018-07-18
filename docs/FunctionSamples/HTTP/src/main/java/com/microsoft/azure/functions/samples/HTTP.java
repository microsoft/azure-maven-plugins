package com.microsoft.azure.functions.samples;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP trigger.
 */
public class HTTP {
    /**
     * This function will listen at HTTP endpoint "/api/HttpTriggerJava". Two approaches to invoke it using "curl" command in bash:
     *   1. curl -d "Http Body" {your host}/api/HttpTriggerJava
     *   2. curl {your host}/api/HttpTriggerJava?name=World
     */
    @FunctionName("HttpTriggerJava")
    public HttpResponseMessage<String> httpHandler(
            @HttpTrigger(name = "req", methods = { "get","post" }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameters
        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponse(400, "Please pass a name on the query string or in the request body");
        } else {
            return request.createResponse(200, "Hello, " + name);
        }
    }
}
