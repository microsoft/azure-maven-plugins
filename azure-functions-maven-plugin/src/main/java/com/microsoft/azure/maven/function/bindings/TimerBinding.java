/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.TimerTrigger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TimerBinding extends BaseBinding {
    private String schedule;

    public TimerBinding(final TimerTrigger timerTrigger) {
        setDirection("in");
        setType("timerTrigger");
        setName(timerTrigger.name());

        schedule = timerTrigger.schedule();
    }

    @JsonGetter
    public String getSchedule() {
        return schedule;
    }
}
