/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import lombok.Data;

@Data
public class DatabaseConfig {
    private String name;
    private Integer throughput;
    private Integer maxThroughput;
}
