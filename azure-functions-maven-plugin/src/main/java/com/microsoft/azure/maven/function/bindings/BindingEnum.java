/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

public enum BindingEnum {
    BLOB_TRIGGER("blobTrigger", Direction.IN, true),
    BLOB_INPUT("blob", Direction.IN, true),
    BLOB_OUTPUT("blob", Direction.OUT, true),
    COSMOSDB_INPUT("cosmosDB", Direction.IN),
    COSMOSDB_OUTPUT("cosmosDB", Direction.OUT),
    COSMOSDB_TRIGGER("cosmosDBTrigger", Direction.IN),
    EVENTHUB_TRIGGER("eventHubTrigger", Direction.IN),
    EVENTHUB_OUTPUT("eventHub", Direction.OUT),
    EVENTGRID_TRIGGER("eventGridTrigger", Direction.IN),
    HTTP_TRIGGER("httpTrigger", Direction.IN),
    HTTP_OUTPUT("http", Direction.OUT),
    QUEUE_TRIGGER("queueTrigger", Direction.IN, true),
    QUEUE_OUTPUT("queue", Direction.OUT, true),
    SENDGRID_OUTPUT("sendGrid", Direction.OUT),
    SERVICEBUSQUEUE_TRIGGER("serviceBusTrigger", Direction.IN),
    SERVICEBUSQUEUE_OUTPUT("serviceBus", Direction.OUT),
    SERVICEBUSTOPIC_TRIGGER("serviceBusTrigger", Direction.IN),
    SERVICEBUSTOPIC_OUTPUT("serviceBus", Direction.OUT),
    TABLE_INPUT("table", Direction.IN, true),
    TABLE_OUTPUT("table", Direction.OUT, true),
    TIMER_TRIGGER("timerTrigger", Direction.IN),
    TWILIOSMS_OUTPUT("twilioSms", Direction.OUT);

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
