/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueueTriggerConfiguration {
    private int maxPollingInterval;

    private String visibilityTimeout;

    private int batchSize;

    private int maxDequeueCount;

    private int newBatchThreshold;

    @JsonGetter("maxPollingInterval")
    public int getMaxPollingInterval() {
        return maxPollingInterval;
    }

    public void setMaxPollingInterval(int maxPollingInterval) {
        this.maxPollingInterval = maxPollingInterval;
    }

    @JsonGetter("visibilityTimeout")
    public String getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(String visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    @JsonGetter("batchSize")
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @JsonGetter("maxDequeueCount")
    public int getMaxDequeueCount() {
        return maxDequeueCount;
    }

    public void setMaxDequeueCount(int maxDequeueCount) {
        this.maxDequeueCount = maxDequeueCount;
    }

    @JsonGetter("newBatchThreshold")
    public int getNewBatchThreshold() {
        return newBatchThreshold;
    }

    public void setNewBatchThreshold(int newBatchThreshold) {
        this.newBatchThreshold = newBatchThreshold;
    }
}
