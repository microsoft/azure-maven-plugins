/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.function.invoker.CommonUtils
import com.microsoft.azure.maven.function.invoker.storage.EventHubProcesser

String functionName = "maven-functions-it-${timestamp}-3"
String storageName = "cihub${timestamp}"
String namespaceName = "FunctionCIEventHubNamespace-${timestamp}"
String resourceGroupName = "maven-functions-it-${timestamp}-rg-3"

CommonUtils.azureLogin()
CommonUtils.deleteAzureResourceGroup(resourceGroupName, true)

// Create EventHub
EventHubProcesser eventHubProcesser = null
eventHubProcesser = new EventHubProcesser(resourceGroupName, namespaceName, storageName);
eventHubProcesser.createOrGetEventHubByName("trigger")
eventHubProcesser.createOrGetEventHubByName("output")

// Get connnection string of EventHub and save it to pom
def connectionString = eventHubProcesser.getEventHubConnectionString()

// Create FunctionApp and set eventhub connection string
CommonUtils.executeCommand("az functionapp create --resource-group ${resourceGroupName} --consumption-plan-location westus \\\n" +
        "--name ${functionName} --storage-account  ${storageName} --runtime java ")

CommonUtils.executeCommand("az webapp config appsettings set --name ${functionName} --resource-group ${resourceGroupName} --settings CIEventHubConnection=\"${connectionString}\"")


return true