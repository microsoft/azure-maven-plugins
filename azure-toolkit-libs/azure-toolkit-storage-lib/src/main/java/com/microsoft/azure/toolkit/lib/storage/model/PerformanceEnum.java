/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PerformanceEnum {

    Standard("Standard", "Recommended for most scenarios (general-purpose v2 account)"),
    Premium("Premium", "Recommended for scenarios that require low latency");

    private String code;
    private String desc;
}
