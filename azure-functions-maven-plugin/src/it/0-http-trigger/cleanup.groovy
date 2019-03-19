/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.function.invoker.CommonUtils

// Verify Azure Functions
def url = "https://maven-functions-it-${timestamp}-0.azurewebsites.net/api/HttpTrigger-Java?json={\"body\":\"Azure\"}".toURL()

CommonUtils.runVerification(new Runnable() {
    @Override
    void run() {
        def response = url.getText()
        assert response == "Hello, Azure"
    }
})

HashMap<String,String> testList = new HashMap<>(new HashMap<String, String>());

// Clean up resources created in test
CommonUtils.deleteAzureResourceGroup("maven-functions-it-${timestamp}-rg-0", false)

return true
