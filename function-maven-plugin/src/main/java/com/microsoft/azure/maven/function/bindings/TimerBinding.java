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

    private boolean runOnStartup;

    private boolean useMonitor;

    public TimerBinding(final TimerTrigger timerTrigger) {
        setDirection("in");
        setType("timerTrigger");
        setName(timerTrigger.name());

        schedule = timerTrigger.schedule();
        runOnStartup = timerTrigger.runOnStartup();
        useMonitor = timerTrigger.useMonitor();
    }

    @JsonGetter("schedule")
    public String getSchedule() {
        return schedule;
    }

    @JsonGetter("runOnStartUp")
    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    @JsonGetter("useMonitor")
    public boolean isUseMonitor() {
        return useMonitor;
    }
}
