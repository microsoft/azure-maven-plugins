/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.bindings;

import java.util.Locale;

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
    EventGridOutput("eventGrid", Direction.OUT),
    HttpTrigger("httpTrigger", Direction.IN),
    HttpOutput("http", Direction.OUT),
    KafkaTrigger("kafkaTrigger", Direction.IN),
    KafkaOutput("kafka", Direction.OUT),
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
    TwilioSmsOutput("twilioSms", Direction.OUT),
    CustomBinding("customBinding", null),
    ExtendedCustomBinding(null, null);

    enum Direction {
        IN, OUT;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ENGLISH);
        }

        public static Direction fromString(String direction) {
            for (final Direction d : Direction.values()) {
                if (d.toString().equalsIgnoreCase(direction)) {
                    return d;
                }
            }
            throw new RuntimeException("Invalid direction is provided");
        }
    }

    private String type;
    private Direction direction;
    private boolean isStorage;

    BindingEnum(String type, Direction direction) {
        this.type = type;
        this.direction = direction;
    }

    BindingEnum(String type, Direction direction, boolean isStorage) {
        this.type = type;
        this.direction = direction;
        this.isStorage = isStorage;
    }

    public String getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isStorage() {
        return isStorage;
    }
}
