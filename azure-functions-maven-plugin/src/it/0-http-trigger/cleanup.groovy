/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.function.invoker.CommonUtils

// Verify Azure Functions
def url = "https://maven-functions-it-${timestamp}-0.azurewebsites.net/api/HttpTriggerJava?json={\"body\":\"Azure\"}".toURL()

// Functions need some time to warm up
int i = 0
while (i < 5) {
    try {
        def response = url.getText()
        assert response == "Hello, Azure"
        break
    } catch (Exception e) {
        e.printStackTrace()
        // ignore warm-up exception and wait for 5 seconds
        i++
        sleep(5000)
    }
}

if (i >= 5) {
    throw new Exception("Integration test fail for Azure Functions http-trigger")
}

// Clean up resources created in test
CommonUtils.deleteAzureResourceGroup("maven-functions-it-rg-0", false)

return true
