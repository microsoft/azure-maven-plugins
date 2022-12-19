/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.CosmosDBManagementClient;
import com.azure.resourcemanager.cosmos.fluent.models.MongoDBDatabaseGetResultsInner;
import com.azure.resourcemanager.cosmos.models.MongoDBDatabaseCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.MongoDBDatabaseResource;
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

public class MongoDatabaseDraft extends MongoDatabase implements
    ICosmosDatabaseDraft<MongoDatabase, MongoDBDatabaseGetResultsInner> {

    @Setter
    @Getter
    private DatabaseConfig config;

    protected MongoDatabaseDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MongoDatabaseModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.create_mongo_database.database", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public MongoDBDatabaseGetResultsInner createResourceInAzure() {
        final CosmosDBManagementClient cosmosDBManagementClient = Objects.requireNonNull(getParent().getRemote()).manager().serviceClient();
        final MongoDBDatabaseCreateUpdateParameters parameters = new MongoDBDatabaseCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(new MongoDBDatabaseResource().withId(this.getName()));
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating MongoDB database({0})...", this.getName()));
        final MongoDBDatabaseGetResultsInner result = cosmosDBManagementClient.getMongoDBResources().createUpdateMongoDBDatabase(this.getResourceGroupName(), this.getParent().getName(),
                this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("MongoDB database({0}) is successfully created.", this.getName()));
        return result;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.update_mongo_database.database", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public MongoDBDatabaseGetResultsInner updateResourceInAzure(@Nonnull MongoDBDatabaseGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getThroughput(), config.getMaxThroughput(), config.getName());
    }

    @Nullable
    @Override
    public MongoDatabase getOrigin() {
        return null;
    }

    private DatabaseConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(DatabaseConfig::new);
        return this.config;
    }

}
