/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.EventGridTrigger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EventGridBinding extends BaseBinding {
    public static final String EVENT_GRID_TRIGGER = "eventGridTrigger";

    public EventGridBinding(final EventGridTrigger eventGridTrigger) {
        super(eventGridTrigger.name(), EVENT_GRID_TRIGGER, Direction.IN, eventGridTrigger.dataType());
    }
}
