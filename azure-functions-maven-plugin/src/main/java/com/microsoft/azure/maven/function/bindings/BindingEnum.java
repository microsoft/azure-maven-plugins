/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

public enum BindingEnum {
    // Enums here are correspond to annotations in com.microsoft.azure.functions.annotation
    BlobTrigger("blobTrigger", Direction.IN, true),
    BlobInput("blob", Direction.IN, true),
    BlobOutput("blob", Direction.OUT, true),
    CosmosDBInput("cosmosDB", Direction.IN),
    CosmosDBOutput("cosmosDB", Direction.OUT),
    CosmosDBTrigger("cosmosDBTrigger", Direction.IN),
    EventHubTrigger("eventHubTrigger", Direction.IN),
    EventHubOutput("eventHub", Direction.OUT),
    EventGridTrigger("eventGridTrigger", Direction.IN),
    HttpTrigger("httpTrigger", Direction.IN),
    HttpOutput("http", Direction.OUT),
    QueueTrigger("queueTrigger", Direction.IN, true),
    QueueOutput("queue", Direction.OUT, true),
    SendGridOutput("sendGrid", Direction.OUT),
    ServiceBusQueueTrigger("serviceBusTrigger", Direction.IN),
    ServiceBusQueueOutput("serviceBus", Direction.OUT),
    ServiceBusTopicTrigger("serviceBusTrigger", Direction.IN),
    ServiceBusTopicOutput("serviceBus", Direction.OUT),
    TableInput("table", Direction.IN, true),
    TableOutput("table", Direction.OUT, true),
    TimerTrigger("timerTrigger", Direction.IN),
    TwilioSmsOutput("twilioSms", Direction.OUT);

    static class Direction {
        static final String IN = "in";
        static final String OUT = "out";
    }

    private String type;
    private String direction;
    private boolean isStorage;

    BindingEnum(String type, String direction) {
        this.type = type;
        this.direction = direction;
    }

    BindingEnum(String type, String direction, boolean isStorage) {
        this.type = type;
        this.direction = direction;
        this.isStorage = isStorage;
    }

    public String getType() {
        return type;
    }

    public String getDirection() {
        return direction;
    }

    public boolean isStorage() {
        return isStorage;
    }
}
