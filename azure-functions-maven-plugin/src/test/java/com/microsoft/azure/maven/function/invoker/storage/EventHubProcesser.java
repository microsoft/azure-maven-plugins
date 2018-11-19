/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.invoker.storage;

import com.google.gson.Gson;
import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventPosition;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.eventhub.EventHub;
import com.microsoft.azure.management.eventhub.EventHubNamespace;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.microsoft.azure.maven.function.invoker.CommonUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EventHubProcesser {

    private static final String SAS_KAY_NAME = "RootManageSharedAccessKey";

    private EventHubNamespace eventHubNamespace;
    private StorageAccount storageAccount;
    private ResourceGroup resourceGroup;

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Map<String, EventHubClient> eventHubClientMap = new HashMap<>();

    public EventHubProcesser(String resourceGroupName, String namespaceName, String storageAccountName)
            throws Exception {
        final Azure azureClint = CommonUtils.getAzureClient();

        if (azureClint.resourceGroups().contain(resourceGroupName)) {
            resourceGroup = azureClint.resourceGroups().getByName(resourceGroupName);
        } else {
            resourceGroup = azureClint.resourceGroups().define(resourceGroupName).withRegion(Region.US_EAST).create();
        }

        final boolean isEventHubNamespaceExist = azureClint.eventHubNamespaces().list().stream()
                .anyMatch(namespace -> namespace.name().equals(namespaceName) &&
                        namespace.resourceGroupName().equals(resourceGroupName));
        if (isEventHubNamespaceExist) {
            eventHubNamespace = azureClint.eventHubNamespaces().getByResourceGroup(resourceGroupName, namespaceName);
        } else {
            eventHubNamespace = azureClint.eventHubNamespaces()
                    .define(namespaceName).withRegion(resourceGroup.region())
                    .withExistingResourceGroup(resourceGroupName).create();
        }

        storageAccount = azureClint.storageAccounts().getByResourceGroup(resourceGroupName, storageAccountName);
        if (storageAccount == null) {
            storageAccount = azureClint.storageAccounts()
                    .define(storageAccountName).withRegion(resourceGroup.region())
                    .withExistingResourceGroup(resourceGroup)
                    .withSku(StorageAccountSkuType.STANDARD_LRS)
                    .withGeneralPurposeAccountKindV2().create();
        }
    }

    public EventHub createOrGetEventHubByName(String eventHubName) throws Exception {
        final Azure azureClient = CommonUtils.getAzureClient();
        final List<EventHub> resultList = eventHubNamespace.listEventHubs().stream()
                .filter(eventHub -> eventHub.name().equals(eventHubName)).collect(Collectors.toList());
        if (resultList.size() == 0) {
            return azureClient.eventHubs().define(eventHubName).withExistingNamespace(eventHubNamespace)
                    .withExistingStorageAccountForCapturedData(storageAccount, eventHubName)
                    .withDataCaptureEnabled().create();
        } else {
            return resultList.get(0);
        }
    }

    public void sendMessageToEventHub(String eventhub, String message) throws Exception {
        final EventHubClient ehClient = getEventHubClientByName(eventhub);
        final String payload = message;
        final Gson gson = new Gson();
        final byte[] payloadBytes = gson.toJson(payload).getBytes(Charset.defaultCharset());
        final EventData sendEvent = EventData.create(payloadBytes);
        ehClient.send(sendEvent).get();
    }

    public List<String> getMessageFromEventHub(String eventhub) throws Exception {
        final List<String> result = new ArrayList<>();
        final EventHubClient ehClient = getEventHubClientByName(eventhub);
        final List<String> partitionIds = Arrays.asList(ehClient.getRuntimeInformation().get().getPartitionIds());
        partitionIds.parallelStream()
                .forEach(partitionId -> result.addAll(receiveMessageFromSpecificPartition(ehClient, partitionId)));
        return result;
    }

    public List<String> receiveMessageFromSpecificPartition(EventHubClient eventHubClient, String partitionId) {
        final List<String> result = new ArrayList<>();
        try {
            final PartitionReceiver partitionReceiver = eventHubClient
                    .createEpochReceiver(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                            partitionId, EventPosition.fromStartOfStream(), 1000).get();
            final Iterable<EventData> data = partitionReceiver.receive(10).get();
            if (data != null) {
                data.forEach(eventData -> result.add(new String(eventData.getBytes())));
            }
            partitionReceiver.closeSync();
        } catch (Exception e) {
            // When exception, just return empty List
            e.printStackTrace();
        }
        return result;
    }

    public void close() throws Exception {
        for (final EventHubClient eventHubClient : eventHubClientMap.values()) {
            eventHubClient.closeSync();
        }
        executorService.shutdown();
    }

    private EventHubClient getEventHubClientByName(String eventHubName) throws Exception {
        if (!eventHubClientMap.containsKey(eventHubName)) {
            final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
                    .setNamespaceName(eventHubNamespace.name())
                    .setEventHubName(eventHubName)
                    .setSasKeyName(SAS_KAY_NAME)
                    .setSasKey(getEventHubKey());
            final EventHubClient ehClient = EventHubClient.create(connStr.toString(), executorService).get();
            eventHubClientMap.put(eventHubName, ehClient);
        }
        return eventHubClientMap.get(eventHubName);
    }

    private String getEventHubKey() {
        return eventHubNamespace.listAuthorizationRules().get(0).getKeys().primaryKey();
    }

    public String getEventHubConnectionString() {
        return eventHubNamespace.listAuthorizationRules().get(0).getKeys().primaryConnectionString();
    }
}
