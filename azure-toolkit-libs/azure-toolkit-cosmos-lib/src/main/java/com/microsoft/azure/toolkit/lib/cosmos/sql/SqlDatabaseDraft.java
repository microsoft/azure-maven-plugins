/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.CosmosDBManagementClient;
import com.azure.resourcemanager.cosmos.fluent.models.SqlDatabaseGetResultsInner;
import com.azure.resourcemanager.cosmos.models.AutoscaleSettings;
import com.azure.resourcemanager.cosmos.models.CreateUpdateOptions;
import com.azure.resourcemanager.cosmos.models.SqlDatabaseCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.SqlDatabaseResource;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabaseDraft;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class SqlDatabaseDraft extends SqlDatabase implements
        ICosmosDatabaseDraft<SqlDatabase, SqlDatabaseGetResultsInner> {

    @Setter
    @Getter
    private DatabaseConfig config;

    protected SqlDatabaseDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull SqlDatabaseModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @NotNull
    @Override
    public SqlDatabaseGetResultsInner createResourceInAzure() {
        final CosmosDBManagementClient cosmosDBManagementClient = Objects.requireNonNull(getParent().getRemote()).manager().serviceClient();
        final SqlDatabaseCreateUpdateParameters parameters = new SqlDatabaseCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(new SqlDatabaseResource().withId(this.getName()));
        final Integer throughput = ensureConfig().getThroughput();
        final Integer maxThroughput = ensureConfig().getMaxThroughput();
        assert ObjectUtils.anyNull(throughput, maxThroughput);
        if (ObjectUtils.anyNotNull(throughput, maxThroughput)) {
            final CreateUpdateOptions options = new CreateUpdateOptions();
            if (Objects.nonNull(ensureConfig().getThroughput())) {
                options.withThroughput(throughput);
            } else {
                options.withAutoscaleSettings(new AutoscaleSettings().withMaxThroughput(maxThroughput));
            }
            parameters.withOptions(options);
        }
        AzureMessager.getMessager().info(AzureString.format("Start creating database({0})...", this.getName()));
        final SqlDatabaseGetResultsInner result = cosmosDBManagementClient.getSqlResources().createUpdateSqlDatabase(this.getResourceGroupName(), this.getParent().getName(),
                this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("Database({0}) is successfully created.", this.getName()));
        return result;
    }

    @NotNull
    @Override
    public SqlDatabaseGetResultsInner updateResourceInAzure(@NotNull SqlDatabaseGetResultsInner origin) {
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
