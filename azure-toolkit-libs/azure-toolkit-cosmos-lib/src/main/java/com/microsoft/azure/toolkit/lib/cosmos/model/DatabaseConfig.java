/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Data;

@Data
public class DatabaseConfig {
    private String name;
    private Integer throughput;
    private Integer maxThroughput;

    public static DatabaseConfig getDefaultDatabaseConfig() {
        return getDefaultDatabaseConfig("database");
    }

    public static DatabaseConfig getDefaultDatabaseConfig(final String prefix) {
        final DatabaseConfig result = new DatabaseConfig();
        result.setName(String.format("%s%s", prefix, Utils.getTimestamp()));
        result.setMaxThroughput(4000);
        return result;
    }
}
