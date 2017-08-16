/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HttpTriggerConfiguration {
    private String routePrefix;

    private int maxOutstandingRequests;

    private int maxConcurrentRequests;

    private boolean dynamicThrottlesEnabled;

    @JsonGetter("routePrefix")
    public String getRoutePrefix() {
        return routePrefix;
    }

    public void setRoutePrefix(String routePrefix) {
        this.routePrefix = routePrefix;
    }

    @JsonGetter("maxOutstandingRequests")
    public int getMaxOutstandingRequests() {
        return maxOutstandingRequests;
    }

    public void setMaxOutstandingRequests(int maxOutstandingRequests) {
        this.maxOutstandingRequests = maxOutstandingRequests;
    }

    @JsonGetter("maxConcurrentRequests")
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    @JsonGetter("dynamicThrottlesEnabled")
    public boolean isDynamicThrottlesEnabled() {
        return dynamicThrottlesEnabled;
    }

    public void setDynamicThrottlesEnabled(boolean dynamicThrottlesEnabled) {
        this.dynamicThrottlesEnabled = dynamicThrottlesEnabled;
    }
}
