/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema of host.json is at https://github.com/Azure/azure-webjobs-sdk-script/blob/dev/schemas/json/host.json
 * Sample host.json is at https://github.com/Azure/azure-webjobs-sdk-script/wiki/host.json
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HostConfiguration {
    private String functionTimeout;

    private HttpTriggerConfiguration http;

    private List<String> watchDirectories = new ArrayList<>();

    private List<String> functions = new ArrayList<>();

    private QueueTriggerConfiguration queues;

    private ServiceBusConfiguration serviceBus;

    private EventHubConfiguration eventHub;

    private TracingConfiguration tracing;

    private SingletonConfiguration singleton;

    @JsonGetter("functionTimeout")
    public String getFunctionTimeout() {
        return functionTimeout;
    }

    public void setFunctionTimeout(String functionTimeout) {
        this.functionTimeout = functionTimeout;
    }

    @JsonGetter("http")
    public HttpTriggerConfiguration getHttp() {
        return http;
    }

    public void setHttp(HttpTriggerConfiguration http) {
        this.http = http;
    }

    @JsonGetter("watchDirectories")
    public List<String> getWatchDirectories() {
        return watchDirectories;
    }

    @JsonGetter("functions")
    public List<String> getFunctions() {
        return functions;
    }

    @JsonGetter("queues")
    public QueueTriggerConfiguration getQueues() {
        return queues;
    }

    public void setQueues(QueueTriggerConfiguration queues) {
        this.queues = queues;
    }

    @JsonGetter("serviceBus")
    public ServiceBusConfiguration getServiceBus() {
        return serviceBus;
    }

    public void setServiceBus(ServiceBusConfiguration serviceBus) {
        this.serviceBus = serviceBus;
    }

    @JsonGetter("eventHub")
    public EventHubConfiguration getEventHub() {
        return eventHub;
    }

    public void setEventHub(EventHubConfiguration eventHub) {
        this.eventHub = eventHub;
    }

    @JsonGetter("tracing")
    public TracingConfiguration getTracing() {
        return tracing;
    }

    public void setTracing(TracingConfiguration tracing) {
        this.tracing = tracing;
    }

    @JsonGetter("singleton")
    public SingletonConfiguration getSingleton() {
        return singleton;
    }

    public void setSingleton(SingletonConfiguration singleton) {
        this.singleton = singleton;
    }
}
