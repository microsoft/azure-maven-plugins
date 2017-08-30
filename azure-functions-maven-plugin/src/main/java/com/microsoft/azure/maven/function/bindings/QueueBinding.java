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
    private String queueName = "";

    public QueueBinding(final QueueTrigger queueTrigger) {
        setDirection("in");
        setType("queueTrigger");
        setName(queueTrigger.name());

        queueName = queueTrigger.queueName();
        setConnection(queueTrigger.connection());
    }

    public QueueBinding(final QueueOutput queueOutput) {
        setDirection("out");
        setType("queue");
        setName(queueOutput.name());

        queueName = queueOutput.queueName();
        setConnection(queueOutput.connection());
    }

    @JsonGetter
    public String getQueueName() {
        return queueName;
    }
}
