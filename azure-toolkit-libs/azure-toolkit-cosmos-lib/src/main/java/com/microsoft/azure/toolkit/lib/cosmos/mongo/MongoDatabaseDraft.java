/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.CosmosDBManagementClient;
import com.azure.resourcemanager.cosmos.fluent.models.MongoDBDatabaseGetResultsInner;
import com.azure.resourcemanager.cosmos.models.AutoscaleSettings;
import com.azure.resourcemanager.cosmos.models.CreateUpdateOptions;
import com.azure.resourcemanager.cosmos.models.MongoDBDatabaseCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.MongoDBDatabaseResource;
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

public class MongoDatabaseDraft extends MongoDatabase implements
        ICosmosDatabaseDraft<MongoDatabase, MongoDBDatabaseGetResultsInner> {

    @Setter
    @Getter
    private DatabaseConfig config;

    protected MongoDatabaseDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull MongoDatabaseModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @NotNull
    @Override
    public MongoDBDatabaseGetResultsInner createResourceInAzure() {
        final CosmosDBManagementClient cosmosDBManagementClient = Objects.requireNonNull(getParent().getRemote()).manager().serviceClient();
        final MongoDBDatabaseCreateUpdateParameters parameters = new MongoDBDatabaseCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(new MongoDBDatabaseResource().withId(this.getName()));
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
        final MongoDBDatabaseGetResultsInner result = cosmosDBManagementClient.getMongoDBResources().createUpdateMongoDBDatabase(this.getResourceGroupName(), this.getParent().getName(),
                this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("Database({0}) is successfully created.", this.getName()));
        return result;
    }

    @NotNull
    @Override
    public MongoDBDatabaseGetResultsInner updateResourceInAzure(@NotNull MongoDBDatabaseGetResultsInner origin) {
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
