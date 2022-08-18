/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.cosmos.CosmosManager;
import com.azure.resourcemanager.cosmos.models.CosmosDBAccount.DefinitionStages.WithConsistencyPolicy;
import com.azure.resourcemanager.cosmos.models.CosmosDBAccount.DefinitionStages.WithKind;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class CosmosDBAccountDraft extends CosmosDBAccount implements
        AzResource.Draft<CosmosDBAccount, com.azure.resourcemanager.cosmos.models.CosmosDBAccount> {

    @Setter
    private Config config;

    protected CosmosDBAccountDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @NotNull
    @Override
    public com.azure.resourcemanager.cosmos.models.CosmosDBAccount createResourceInAzure() {
        final CosmosManager remote = this.getParent().getRemote();
        final DatabaseAccountKind kind = Objects.requireNonNull(getKind(), "'kind' is required to create Azure Cosmos DB account");
        final Region region = Objects.requireNonNull(getRegion(), "'region' is required to create Azure Cosmos DB account");
        final WithKind withKind = Objects.requireNonNull(remote).databaseAccounts().define(this.getName())
                .withRegion(region.getName())
                .withExistingResourceGroup(this.getResourceGroupName());
        final WithConsistencyPolicy withConsistencyPolicy;
        if (Objects.equals(kind, DatabaseAccountKind.SQL)) {
            withConsistencyPolicy = withKind.withDataModelSql();
        } else if (Objects.equals(kind, DatabaseAccountKind.MONGO_DB)) {
            withConsistencyPolicy = withKind.withDataModelMongoDB();
        } else if (Objects.equals(kind, DatabaseAccountKind.CASSANDRA)) {
            withConsistencyPolicy = withKind.withDataModelCassandra();
        } else {
            throw new AzureToolkitRuntimeException(String.format("kind %s is not supported for Cosmos DB account", kind.getValue()));
        }
        return withConsistencyPolicy.withSessionConsistency()
                .withWriteReplication(com.azure.core.management.Region.fromName(region.getName()))
                .create();
    }

    @NotNull
    @Override
    public com.azure.resourcemanager.cosmos.models.CosmosDBAccount updateResourceInAzure(@NotNull com.azure.resourcemanager.cosmos.models.CosmosDBAccount origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getSubscription(), config.getResourceGroup(), config.getName(), config.getKind(), config.getRegion());
    }

    @Nullable
    @Override
    public CosmosDBAccount getOrigin() {
        return null;
    }

    public Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(Config::new);
        return this.config;
    }

    @Nullable
    @Override
    public DatabaseAccountKind getKind() {
        return Optional.ofNullable(config).map(Config::getKind).orElseGet(super::getKind);
    }

    @Nullable
    @Override
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion);
    }

    @Data
    public static class Config {
        private Subscription subscription;
        private String name;
        private ResourceGroup resourceGroup;
        private Region region;
        private DatabaseAccountKind kind;
    }
}
