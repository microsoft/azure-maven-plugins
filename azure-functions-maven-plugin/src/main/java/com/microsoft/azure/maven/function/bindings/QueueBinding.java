/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.QueueOutput;
import com.microsoft.azure.serverless.functions.annotation.QueueTrigger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueueBinding extends StorageBaseBinding {
    public static final String QUEUE_TRIGGER = "queueTrigger";
    public static final String QUEUE = "queue";

    private String queueName = "";

    public QueueBinding(final QueueTrigger queueTrigger) {
        super(queueTrigger.name(), QUEUE_TRIGGER, Direction.IN, queueTrigger.dataType());

        queueName = queueTrigger.queueName();
        setConnection(queueTrigger.connection());
    }

    public QueueBinding(final QueueOutput queueOutput) {
        super(queueOutput.name(), QUEUE, Direction.OUT, queueOutput.dataType());

        queueName = queueOutput.queueName();
        setConnection(queueOutput.connection());
    }

    @JsonGetter
    public String getQueueName() {
        return queueName;
    }
}
