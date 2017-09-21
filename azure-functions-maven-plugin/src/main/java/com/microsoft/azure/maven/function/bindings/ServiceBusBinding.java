/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.serverless.functions.annotation.ServiceBusQueueTrigger;
import com.microsoft.azure.serverless.functions.annotation.ServiceBusTopicOutput;
import com.microsoft.azure.serverless.functions.annotation.ServiceBusTopicTrigger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceBusBinding extends BaseBinding {
    private String queueName = "";

    private String topicName = "";

    private String subscriptionName = "";

    private String connection = "";

    private String access = "";

    public ServiceBusBinding(final ServiceBusQueueTrigger queueTrigger) {
        super(queueTrigger.name(), "serviceBusTrigger", Direction.IN);

        queueName = queueTrigger.queueName();
        connection = queueTrigger.connection();
        access = queueTrigger.access().toString();
    }

    public ServiceBusBinding(final ServiceBusTopicTrigger topicTrigger) {
        super(topicTrigger.name(), "serviceBusTrigger", Direction.IN);

        topicName = topicTrigger.topicName();
        subscriptionName = topicTrigger.subscriptionName();
        connection = topicTrigger.connection();
        access = topicTrigger.access().toString();
    }

    public ServiceBusBinding(final ServiceBusQueueOutput queueOutput) {
        super(queueOutput.name(), "serviceBus", Direction.OUT);

        queueName = queueOutput.queueName();
        connection = queueOutput.connection();
        access = queueOutput.access().toString();
    }

    public ServiceBusBinding(final ServiceBusTopicOutput topicOutput) {
        super(topicOutput.name(), "serviceBus", Direction.OUT);

        topicName = topicOutput.topicName();
        subscriptionName = topicOutput.subscriptionName();
        connection = topicOutput.connection();
        access = topicOutput.access().toString();
    }

    @JsonGetter
    public String getQueueName() {
        return queueName;
    }

    @JsonGetter
    public String getTopicName() {
        return topicName;
    }

    @JsonGetter
    public String getSubscriptionName() {
        return subscriptionName;
    }

    @JsonGetter
    public String getAccess() {
        return access;
    }

    @JsonGetter
    public String getConnection() {
        return connection;
    }
}
