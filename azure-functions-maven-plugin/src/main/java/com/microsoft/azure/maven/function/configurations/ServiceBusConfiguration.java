/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceBusConfiguration {
    private int maxConcurrentCalls;

    private int prefetchCount;

    private String autoRenewTimeout;

    @JsonGetter("maxConcurrentCalls")
    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public void setMaxConcurrentCalls(int maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    @JsonGetter("prefetchCount")
    public int getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(int prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    @JsonGetter("autoRenewTimeout")
    public String getAutoRenewTimeout() {
        return autoRenewTimeout;
    }

    public void setAutoRenewTimeout(String autoRenewTimeout) {
        this.autoRenewTimeout = autoRenewTimeout;
    }
}
