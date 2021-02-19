/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public EventHub createOrGetEventHubByName(final String eventHubName) throws Exception {
        final Azure azureClient = CommonUtils.getAzureClient();
        final Optional<EventHub> eventHub = eventHubNamespace.listEventHubs().stream()
            .filter(eventHubEntry -> eventHubEntry.name().equals(eventHubName)).findFirst();
        return eventHub.isPresent() ? eventHub.get() : azureClient.eventHubs().define(eventHubName)
            .withExistingNamespace(eventHubNamespace)
            .withExistingStorageAccountForCapturedData(storageAccount, eventHubName)
            .withDataCaptureEnabled().create();
    }

    public void sendMessageToEventHub(final String eventHubName, final String message) throws Exception {
        final EventHubClient eventHubClient = getEventHubClientByName(eventHubName);
        final Gson gson = new Gson();
        final byte[] payloadBytes = gson.toJson(message).getBytes(Charset.defaultCharset());
        final EventData sendEvent = EventData.create(payloadBytes);
        eventHubClient.send(sendEvent).get();
    }

    public List<String> getMessageFromEventHub(final String eventHubName) throws Exception {
        final List<String> result = new CopyOnWriteArrayList<>();
        final EventHubClient eventHubClient = getEventHubClientByName(eventHubName);
        final List<String> partitionIds = Arrays.asList(eventHubClient.getRuntimeInformation().get().getPartitionIds());
        partitionIds.parallelStream()
            .forEach(partitionId -> result.addAll(getMessageFromPartition(eventHubClient, partitionId)));
        return result;
    }

    public List<String> getMessageFromPartition(final EventHubClient eventHubClient, final String partitionId) {
        final List<String> result = new ArrayList<>();
        try {
            final PartitionReceiver partitionReceiver = eventHubClient
                .createReceiver(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                    partitionId, EventPosition.fromStartOfStream()).get();
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

    private EventHubClient getEventHubClientByName(final String eventHubName) throws Exception {
        if (eventHubClientMap.containsKey(eventHubName)) {
            return eventHubClientMap.get(eventHubName);
        } else {
            final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
                .setNamespaceName(eventHubNamespace.name())
                .setEventHubName(eventHubName)
                .setSasKeyName(SAS_KAY_NAME)
                .setSasKey(getEventHubKey());
            final EventHubClient eventHubClient = EventHubClient.create(connStr.toString(), executorService).get();
            eventHubClientMap.put(eventHubName, eventHubClient);
            return eventHubClient;
        }
    }

    private String getEventHubKey() {
        return eventHubNamespace.listAuthorizationRules().get(0).getKeys().primaryKey();
    }

    public String getEventHubConnectionString() {
        return eventHubNamespace.listAuthorizationRules().get(0).getKeys().primaryConnectionString();
    }
}
