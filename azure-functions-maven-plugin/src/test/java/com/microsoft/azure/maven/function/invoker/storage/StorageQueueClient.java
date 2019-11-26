/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.invoker.storage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

public class StorageQueueClient {
    private CloudQueueClient queueClient;

    public void init(String connectionString) throws Exception {
        final CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        this.queueClient = storageAccount.createCloudQueueClient();
    }

    public CloudQueue createQueueWithName(String name) throws Exception {
        if (this.queueClient == null) {
            throw new Exception("CloudQueueClient is not initialized");
        }
        final CloudQueue queue = queueClient.getQueueReference(name);
        queue.createIfNotExists();
        return queue;
    }

    public void sendMessageTo(String queueName, String msg) throws Exception {
        final CloudQueue queue = queueClient.getQueueReference(queueName);
        final CloudQueueMessage message = new CloudQueueMessage(msg);
        queue.addMessage(message);
    }

    public String peekNextMessageFrom(String queueName) throws Exception {
        final CloudQueue queue = queueClient.getQueueReference(queueName);
        final CloudQueueMessage peekedMessage = queue.peekMessage();
        if (peekedMessage != null) {
            return peekedMessage.getMessageContentAsString();
        }
        throw new Exception("No new message in the queue.");
    }

}
