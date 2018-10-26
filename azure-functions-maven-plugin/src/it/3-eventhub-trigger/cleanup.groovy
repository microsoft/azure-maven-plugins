package com.microsoft.azure

import com.google.gson.Gson
import com.microsoft.azure.maven.function.invoker.storage.EventProcessor
import com.microsoft.azure.maven.function.invoker.CommonUtils
import com.microsoft.azure.eventhubs.ConnectionStringBuilder
import com.microsoft.azure.eventhubs.EventData
import com.microsoft.azure.eventhubs.EventHubClient
import com.microsoft.azure.eventprocessorhost.EventProcessorHost
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.util.concurrent.Executors

String consumerGroupName = "\$Default"
String functionName = "maven-functions-it-${timestamp}-3"
String storageName = "ncihub${timestamp}"
String nameSpace = "CIEventHubNamespace-${timestamp}"
String resourceGroup = "maven-functions-it-rg-3"

// Create Eventhub and set the connection string for function
CommonUtils.executeCommand(
        "az eventhubs namespace create --name ${nameSpace} --resource-group ${resourceGroup} -l eastus2")

CommonUtils.executeCommand(
        "az eventhubs eventhub create --name trigger --resource-group ${resourceGroup} --namespace-name ${nameSpace}")

CommonUtils.executeCommand(
        "az eventhubs eventhub create --name output --resource-group ${resourceGroup} --namespace-name ${nameSpace}")

CommonUtils.executeCommand("az storage account create --name ${storageName} --resource-group ${resourceGroup} --location eastus2 --sku Standard_LRS --kind StorageV2")

String storageKeyJsonString = CommonUtils.executeCommand("az storage account keys list --resource-group ${resourceGroup} --account-name ${storageName}")

def jsonSlurper = new JsonSlurper()

def storageKeyJsonObject = jsonSlurper.parseText(storageKeyJsonString)
String storageKey = storageKeyJsonObject[0].value

String eventHubKeys = CommonUtils.executeCommand(
        "az eventhubs namespace authorization-rule keys list --resource-group ${resourceGroup} --namespace-name ${nameSpace} --name RootManageSharedAccessKey")

def eventHubKeysJsonObject = jsonSlurper.parseText(eventHubKeys)
String connectionKey = eventHubKeysJsonObject.primaryConnectionString

CommonUtils.executeCommand("az webapp config appsettings set --name ${functionName} --resource-group ${resourceGroup} --settings CIEventHubConnection=${connectionKey}")

// Register EventHubProcessor
String sasKeyName = "RootManageSharedAccessKey"
String sasKey = connectionKey.split(";")[2].replace("SharedAccessKey=", "")
String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=${storageName};AccountKey=${storageKey}"
ConnectionStringBuilder eventHubConnectionString = new ConnectionStringBuilder()
        .setNamespaceName(nameSpace)
        .setEventHubName("output")
        .setSasKeyName(sasKeyName)
        .setSasKey(sasKey)
EventProcessorHost host = new EventProcessorHost(
        EventProcessorHost.createHostName("EventProcessorHost"),
        "output",
        consumerGroupName,
        eventHubConnectionString.toString(),
        storageConnectionString,
        storageName)
System.out.println("Registering host named " + host.getHostName())
host.registerEventProcessor(EventProcessor.class).get()

// Send trigger message
final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
        .setNamespaceName(nameSpace)
        .setEventHubName("trigger")
        .setSasKeyName(sasKeyName)
        .setSasKey(sasKey)
final EventHubClient ehClient = EventHubClient.createSync(connStr.toString(), Executors.newCachedThreadPool())
String payload = "CIInput"
Gson gson = new Gson()
byte[] payloadBytes = gson.toJson(payload).getBytes(Charset.defaultCharset())
EventData sendEvent = EventData.create(payloadBytes)
ehClient.sendSync(sendEvent)
ehClient.closeSync()

// Verify
Thread.sleep(60 * 1000)
assert EventProcessor.getMessages().get(0).equals("CITest")

CommonUtils.deleteAzureResourceGroup("maven-functions-it-rg-3", false)
return true