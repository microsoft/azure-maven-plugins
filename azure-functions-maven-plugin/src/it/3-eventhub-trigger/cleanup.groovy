/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.function.invoker.storage.StorageQueueClient
import com.microsoft.azure.maven.function.invoker.CommonUtils
import groovy.json.JsonSlurper

// Create Eventhub and set the connection string for function
CommonUtils.executeCommand(
        "az eventhubs namespace create --name CIEventHubNamespace-${timestamp} --resource-group maven-functions-it-rg-3 -l eastus2")
CommonUtils.executeCommand(
        "az eventhubs eventhub create --name CIEventHub --resource-group maven-functions-it-rg-3 --namespace-name CIEventHubNamespace-${timestamp}")
String eventHubKeys = CommonUtils.executeCommand(
        "az eventhubs namespace authorization-rule keys list --resource-group maven-functions-it-rg-3 --namespace-name CIEventHubNamespace-${timestamp} --name RootManageSharedAccessKey")

def jsonSlurper = new JsonSlurper()
def eventHubKeysJsonObject = jsonSlurper.parseText(eventHubKeys)
String connectionKey = eventHubKeysJsonObject.primaryConnectionString

CommonUtils.executeCommand(String.format(
        "az webapp config appsettings set --name maven-functions-it-${timestamp}-3 --resource-group maven-functions-it-rg-3 --settings CIEventHubConnection=%s",
        connectionKey))

// Create verify queue
String azQueryValue = CommonUtils.executeCommand(
        "az functionapp config appsettings list -n \"maven-functions-it-${timestamp}-3\" -g \"maven-functions-it-rg-3\" --query \"[?name=='AzureWebJobsStorage'].value\"")
String storageConnectionString = azQueryValue.split("\"")[1]

StorageQueueClient queueClient = new StorageQueueClient()
queueClient.init(storageConnectionString)
queueClient.createQueueWithName("out")

// Verify through Http request
"https://maven-functions-it-${timestamp}-3.azurewebsites.net/api/HttpTrigger-Java".toURL().getText() // Send http request
String msg = queueClient.peekNextMessageFrom("out")
assert msg == "CITrigger"

// Clean up resources created in test
CommonUtils.deleteAzureResourceGroup("maven-functions-it-rg-3", false)
return true
