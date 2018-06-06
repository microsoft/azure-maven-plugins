/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.google.gson.Gson;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    @FunctionName("HttpTriggerJava")
    public HttpResponseMessage<String> httpTrigger(
            @HttpTrigger(name = "req", methods = {"get", "post"}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("json");
        Gson gson = new Gson();

        try {
            return request.createResponse(200, "Hello, " + gson.fromJson(query, GsonFunctionBody.class).body);
        } catch (Exception e) {
            return request.createResponse(400, "Format error");
        }
    }
}
