/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AzureSpotConfig {
    private final double maximumPrice;
    private final EvictionType type;
    private final EvictionPolicy policy;

    public enum EvictionType {
        CapacityOnly,
        PriceOrCapacity;
    }

    public enum EvictionPolicy {
        StopAndDeallocate,
        Delete;
    }
}
