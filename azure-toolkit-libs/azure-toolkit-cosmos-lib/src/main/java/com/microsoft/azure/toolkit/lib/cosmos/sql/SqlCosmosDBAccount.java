/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountModule;
import com.microsoft.azure.toolkit.lib.cosmos.model.CosmosDBAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.SqlDatabaseAccountConnectionString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqlCosmosDBAccount extends CosmosDBAccount {
    private CosmosClient cosmosClient;
    private final SqlDatabaseModule sqlDatabaseModule;

    public SqlCosmosDBAccount(@NotNull String name, @NotNull String resourceGroupName, @NotNull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
        this.sqlDatabaseModule = new SqlDatabaseModule(this);
    }

    public SqlCosmosDBAccount(@NotNull SqlCosmosDBAccount account) {
        super(account);
        this.sqlDatabaseModule = account.sqlDatabaseModule;
        this.cosmosClient = account.cosmosClient;
    }

    public SqlCosmosDBAccount(@NotNull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote, @NotNull CosmosDBAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.sqlDatabaseModule = new SqlDatabaseModule(this);
    }

    public CosmosClient getClient() {
        if (Objects.isNull(this.cosmosClient)) {
            this.cosmosClient = getCosmosClient();
        }
        return this.cosmosClient;
    }

    @Override
    public @NotNull List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(sqlDatabaseModule);
    }

    public SqlDatabaseModule sqlDatabases() {
        return this.sqlDatabaseModule;
    }

    @Nonnull
    @Override
    public CosmosDBAccountConnectionString getCosmosDBAccountPrimaryConnectionString() {
        return getSqlAccountConnectionString();
    }

    @Nonnull
    public SqlDatabaseAccountConnectionString getSqlAccountConnectionString() {
        return SqlDatabaseAccountConnectionString.builder()
                .connectionString(listConnectionStrings().getPrimaryConnectionString())
                .key(listKeys().getPrimaryMasterKey())
                .uri(getDocumentEndpoint())
                .build();
    }

    @Override
    protected void updateAdditionalProperties(com.azure.resourcemanager.cosmos.models.CosmosDBAccount newRemote, com.azure.resourcemanager.cosmos.models.CosmosDBAccount oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        if (Objects.nonNull(newRemote)) {
            this.cosmosClient = getCosmosClient();
        } else {
            Optional.ofNullable(this.cosmosClient).ifPresent(CosmosClient::close);
            this.cosmosClient = null;
        }
    }

    private CosmosClient getCosmosClient() {
        try {
            final SqlDatabaseAccountConnectionString connectionString = this.getSqlAccountConnectionString();
            return new CosmosClientBuilder()
                    .endpointDiscoveryEnabled(false)
                    .endpoint(this.getDocumentEndpoint())
                    .key(connectionString.getKey())
                    .preferredRegions(Collections.singletonList(Objects.requireNonNull(this.getRegion()).getName()))
                    .consistencyLevel(ConsistencyLevel.EVENTUAL)
                    .buildClient();
        } catch (Throwable e) {
            // swallow exception to load data client
            return null;
        }
    }
}
