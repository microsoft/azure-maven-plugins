/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

import com.microsoft.azure.maven.function.invoker.storage.StorageQueueClient
import com.microsoft.azure.maven.function.invoker.CommonUtils

// Get Connection String for the Function App binding Storage Account
String azQueryValue = CommonUtils.executeCommand("az functionapp config appsettings list -n \"maven-functions-it-${timestamp}-2\" -g \"maven-functions-it-${timestamp}-rg-2\" --query \"[?name=='AzureWebJobsStorage'].value\"")
String storageConnectionString = azQueryValue.split("\"")[1]

StorageQueueClient queueClient = new StorageQueueClient()
queueClient.init(storageConnectionString)
queueClient.createQueueWithName("out")

CommonUtils.runVerification(new Runnable() {
    @Override
    void run() {
        sleep(10 * 1000 /* ms */)
        String msg = queueClient.peekNextMessageFrom("out")
        assert msg == "successfully triggered"
    }
})

// Clean up resources created in test
CommonUtils.deleteAzureResourceGroup("maven-functions-it-${timestamp}-rg-2", false)
return true
