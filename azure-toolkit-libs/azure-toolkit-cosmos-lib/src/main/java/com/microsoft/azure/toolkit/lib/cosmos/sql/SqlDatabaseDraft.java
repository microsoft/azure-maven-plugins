/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.CosmosDBManagementClient;
import com.azure.resourcemanager.cosmos.fluent.models.SqlDatabaseGetResultsInner;
import com.azure.resourcemanager.cosmos.models.SqlDatabaseCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.SqlDatabaseResource;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabaseDraft;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class SqlDatabaseDraft extends SqlDatabase implements
    ICosmosDatabaseDraft<SqlDatabase, SqlDatabaseGetResultsInner> {

    @Setter
    @Getter
    private DatabaseConfig config;

    protected SqlDatabaseDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull SqlDatabaseModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.create_sql_database.database", params = {"this.getName()"})
    public SqlDatabaseGetResultsInner createResourceInAzure() {
        final CosmosDBManagementClient cosmosDBManagementClient = Objects.requireNonNull(getParent().getRemote()).manager().serviceClient();
        final SqlDatabaseCreateUpdateParameters parameters = new SqlDatabaseCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(new SqlDatabaseResource().withId(this.getName()));
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating SQL database({0})...", this.getName()));
        final SqlDatabaseGetResultsInner result = cosmosDBManagementClient.getSqlResources().createUpdateSqlDatabase(this.getResourceGroupName(), this.getParent().getName(),
                this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("SQL database({0}) is successfully created.", this.getName()));
        return result;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.update_sql_database.database", params = {"this.getName()"})
    public SqlDatabaseGetResultsInner updateResourceInAzure(@Nonnull SqlDatabaseGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getThroughput(), config.getMaxThroughput(), config.getName());
    }

    @Nullable
    @Override
    public SqlDatabase getOrigin() {
        return null;
    }

    private DatabaseConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(DatabaseConfig::new);
        return this.config;
    }

}
