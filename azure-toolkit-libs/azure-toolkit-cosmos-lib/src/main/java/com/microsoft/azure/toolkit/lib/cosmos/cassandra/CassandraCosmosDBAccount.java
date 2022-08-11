/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountModule;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountConnectionStrings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

public class CassandraCosmosDBAccount extends CosmosDBAccount {
    private final CassandraKeyspaceModule keyspaceModule;

    public CassandraCosmosDBAccount(@NotNull String name, @NotNull String resourceGroupName, @NotNull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
        this.keyspaceModule = new CassandraKeyspaceModule(this);
    }

    public CassandraCosmosDBAccount(@NotNull CosmosDBAccount account) {
        super(account);
        this.keyspaceModule = new CassandraKeyspaceModule(this);
    }

    public CassandraCosmosDBAccount(@NotNull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote, @NotNull CosmosDBAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.keyspaceModule = new CassandraKeyspaceModule(this);
    }

    public CassandraKeyspaceModule keySpaces() {
        return this.keyspaceModule;
    }

    public String getPrimaryConnectionString() {
        return Optional.ofNullable(listConnectionStrings()).map(DatabaseAccountConnectionStrings::getPrimaryConnectionString).orElse(null);
    }

    public Integer getPort() {
        return Optional.ofNullable(getPrimaryConnectionString())
                .map(connectionString -> getParameterFromConnectionString(connectionString, "Port"))
                .map(Integer::valueOf).orElse(null);
    }

    public String getUserName() {
        return Optional.ofNullable(getPrimaryConnectionString())
                .map(connectionString -> getParameterFromConnectionString(connectionString, "Username")).orElse(null);
    }

    public String getContactPoint() {
        return Optional.ofNullable(getPrimaryConnectionString())
                .map(connectionString -> getParameterFromConnectionString(connectionString, "HostName")).orElse(null);
    }

    public String getParameterFromConnectionString(@Nonnull final String connectionString, final String key) {
        final String[] parameters = connectionString.split(";");
        final String parameter = Arrays.stream(parameters).filter(value -> StringUtils.containsIgnoreCase(value, key)).findFirst().orElse(null);
        return StringUtils.isEmpty(parameter) ? null : StringUtils.substringAfterLast(parameter, "=");
    }
}
