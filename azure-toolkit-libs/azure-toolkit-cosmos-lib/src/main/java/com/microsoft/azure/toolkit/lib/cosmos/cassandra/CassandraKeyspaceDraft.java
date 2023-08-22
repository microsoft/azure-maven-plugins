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
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
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

public class CassandraKeyspaceDraft extends CassandraKeyspace implements
    ICosmosDatabaseDraft<CassandraKeyspace, CassandraKeyspaceGetResultsInner> {

    @Setter
    @Getter
    private DatabaseConfig config;

    protected CassandraKeyspaceDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CassandraKeyspaceModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.create_cassandra_keyspace.keyspace", params = {"this.getName()"})
    public CassandraKeyspaceGetResultsInner createResourceInAzure() {
        final CosmosDBManagementClient cosmosDBManagementClient = Objects.requireNonNull(getParent().getRemote()).manager().serviceClient();
        final CassandraKeyspaceCreateUpdateParameters parameters = new CassandraKeyspaceCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(new CassandraKeyspaceResource().withId(this.getName()));
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating Cassandra keyspace({0})...", this.getName()));
        final CassandraKeyspaceGetResultsInner result = cosmosDBManagementClient.getCassandraResources().createUpdateCassandraKeyspace(this.getResourceGroupName(), this.getParent().getName(),
                this.getName(), parameters, Context.NONE);
        final Action<AzResource> connect = AzureActionManager.getInstance().getAction(Action.CONNECT_RESOURCE).bind(this);
        AzureMessager.getMessager().success(AzureString.format("Cassandra keyspace({0}) is successfully created.", this.getName()), connect);
        return result;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.update_cassandra_keyspace.keyspace", params = {"this.getName()"})
    public CassandraKeyspaceGetResultsInner updateResourceInAzure(@Nonnull CassandraKeyspaceGetResultsInner origin) {
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
