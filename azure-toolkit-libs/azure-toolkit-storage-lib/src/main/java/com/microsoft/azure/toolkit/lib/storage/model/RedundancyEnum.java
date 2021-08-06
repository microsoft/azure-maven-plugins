/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RedundancyEnum {

    Standard_ZRS(PerformanceEnum.Standard, "Standard_ZRS", "Zone-Redundant"),
    Standard_LRS(PerformanceEnum.Standard, "Standard_LRS", "Locally Redundant"),
    Standard_GRS(PerformanceEnum.Standard, "Standard_GRS", "Geo-Redundant"),
    Standard_RAGRS(PerformanceEnum.Standard, "Standard_RAGRS", "Read Access Geo-Redundant"),
    Premium_LRS(PerformanceEnum.Premium, "Premium_LRS", "Locally Redundant");

    private PerformanceEnum performance;
    private String code;
    private String desc;
}
