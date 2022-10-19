/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.CosmosDBManagementClient;
import com.azure.resourcemanager.cosmos.fluent.models.CassandraKeyspaceGetResultsInner;
import com.azure.resourcemanager.cosmos.models.CassandraKeyspaceCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.CassandraKeyspaceResource;
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

public class CassandraKeyspaceDraft extends CassandraKeyspace implements
        ICosmosDatabaseDraft<CassandraKeyspace, CassandraKeyspaceGetResultsInner> {

    @Setter
    @Getter
    private DatabaseConfig config;

    protected CassandraKeyspaceDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull CassandraKeyspaceModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @NotNull
    @Override
    public CassandraKeyspaceGetResultsInner createResourceInAzure() {
        final CosmosDBManagementClient cosmosDBManagementClient = Objects.requireNonNull(getParent().getRemote()).manager().serviceClient();
        final CassandraKeyspaceCreateUpdateParameters parameters = new CassandraKeyspaceCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(new CassandraKeyspaceResource().withId(this.getName()));
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating Cassandra keyspace({0})...", this.getName()));
        final CassandraKeyspaceGetResultsInner result = cosmosDBManagementClient.getCassandraResources().createUpdateCassandraKeyspace(this.getResourceGroupName(), this.getParent().getName(),
                this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("Cassandra keyspace({0}) is successfully created.", this.getName()));
        return result;
    }

    @NotNull
    @Override
    public CassandraKeyspaceGetResultsInner updateResourceInAzure(@NotNull CassandraKeyspaceGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getThroughput(), config.getMaxThroughput(), config.getName());
    }

    @Nullable
    @Override
    public CassandraKeyspace getOrigin() {
        return null;
    }

    private DatabaseConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(DatabaseConfig::new);
        return this.config;
    }

}
