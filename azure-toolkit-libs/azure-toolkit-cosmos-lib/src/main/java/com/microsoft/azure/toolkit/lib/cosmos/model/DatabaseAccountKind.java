/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import com.azure.resourcemanager.cosmos.models.Capability;
import com.azure.resourcemanager.cosmos.models.CosmosDBAccount;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class DatabaseAccountKind {
    public static final DatabaseAccountKind SQL = new DatabaseAccountKind("SQL");
    public static final DatabaseAccountKind MONGO_DB = new DatabaseAccountKind("MongoDB");
    public static final DatabaseAccountKind CASSANDRA = new DatabaseAccountKind("Cassandra");

    private String value;

    public static List<DatabaseAccountKind> values() {
        return Arrays.asList(SQL, MONGO_DB, CASSANDRA);
    }

    public static DatabaseAccountKind fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElseGet(() -> new DatabaseAccountKind(input));
    }

    public static DatabaseAccountKind fromAccount(@Nonnull CosmosDBAccount account) {
        final com.azure.resourcemanager.cosmos.models.DatabaseAccountKind kind = account.kind();
        // for mongo, return directly
        if (kind == com.azure.resourcemanager.cosmos.models.DatabaseAccountKind.MONGO_DB) {
            return DatabaseAccountKind.MONGO_DB;
        }
        // get account kind from capabilities
        final List<Capability> capabilities = account.capabilities();
        return CollectionUtils.isEmpty(capabilities) ? DatabaseAccountKind.SQL : DatabaseAccountKind.fromCapability(capabilities.get(0));
    }

    private static DatabaseAccountKind fromCapability(@Nonnull Capability capability) {
        final String name = capability.name();
        if (StringUtils.equalsIgnoreCase(name, "EnableCassandra")) {
            return CASSANDRA;
        } else {
            return fromString(StringUtils.remove(name, "Enable"));
        }
    }
}
