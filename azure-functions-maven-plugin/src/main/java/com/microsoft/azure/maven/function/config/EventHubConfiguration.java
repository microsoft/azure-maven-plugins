/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EventHubConfiguration {
    private int maxBatchSize;

    private int prefetchCount;

    private int batchCheckpointFrequency;

    @JsonGetter("maxBatchSize")
    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    @JsonGetter("prefetchCount")
    public int getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(int prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    @JsonGetter("batchCheckpointFrequency")
    public int getBatchCheckpointFrequency() {
        return batchCheckpointFrequency;
    }

    public void setBatchCheckpointFrequency(int batchCheckpointFrequency) {
        this.batchCheckpointFrequency = batchCheckpointFrequency;
    }
}
