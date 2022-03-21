/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Availability {
    private boolean available;
    private String unavailabilityReason;
    private String unavailabilityMessage;

    public Availability(boolean available, String unavailabilityReason) {
        this.available = available;
        this.unavailabilityReason = unavailabilityReason;
    }
}
