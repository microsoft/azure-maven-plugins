/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.cosmos.CosmosManager;
import com.azure.resourcemanager.cosmos.models.CosmosDBAccounts;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class CosmosDBAccountModule extends AbstractAzResourceModule<CosmosDBAccount, CosmosServiceSubscription,
        com.azure.resourcemanager.cosmos.models.CosmosDBAccount> {
    private static final String NAME = "databaseAccounts";

    public CosmosDBAccountModule(@Nonnull CosmosServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected CosmosDBAccount newResource(@Nonnull com.azure.resourcemanager.cosmos.models.CosmosDBAccount cosmosDBAccount) {
        final DatabaseAccountKind databaseAccountKind = DatabaseAccountKind.fromAccount(cosmosDBAccount);
        if (Objects.equals(databaseAccountKind, DatabaseAccountKind.SQL)) {
            return new SqlCosmosDBAccount(cosmosDBAccount, this);
        } else if (Objects.equals(databaseAccountKind, DatabaseAccountKind.MONGO_DB)) {
            return new MongoCosmosDBAccount(cosmosDBAccount, this);
        } else if (Objects.equals(databaseAccountKind, DatabaseAccountKind.CASSANDRA)) {
            return new CassandraCosmosDBAccount(cosmosDBAccount, this);
        } else {
            return new CosmosDBAccount(cosmosDBAccount, this);
        }
    }

    @Nonnull
    @Override
    protected CosmosDBAccount newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        final com.azure.resourcemanager.cosmos.models.CosmosDBAccount account = Optional.ofNullable(getClient())
            .map(c -> c.getByResourceGroup(resourceGroupName, name)).orElse(null);
        return account == null ? new CosmosDBAccount(name, Objects.requireNonNull(resourceGroupName), this) : newResource(account);
    }

    @Nullable
    @Override
    public CosmosDBAccounts getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(CosmosManager::databaseAccounts).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure Cosmos DB account";
    }

    @NotNull
    @Override
    protected AzResource.Draft<CosmosDBAccount, com.azure.resourcemanager.cosmos.models.CosmosDBAccount> newDraftForUpdate(@NotNull CosmosDBAccount cosmosDBAccount) {
        throw new UnsupportedOperationException("not support");
    }

    @NotNull
    @Override
    protected AzResource.Draft<CosmosDBAccount, com.azure.resourcemanager.cosmos.models.CosmosDBAccount> newDraftForCreate(@NotNull String name, @org.jetbrains.annotations.Nullable String rgName) {
        return new CosmosDBAccountDraft(name, Objects.requireNonNull(rgName), this);
    }

    @NotNull
    @Override
    public CosmosDBAccount create(@NotNull AzResource.Draft<CosmosDBAccount, com.azure.resourcemanager.cosmos.models.CosmosDBAccount> draft) {
        final CosmosDBAccount draftAccount = super.create(draft);
        this.deleteResourceFromLocal(draftAccount.getId()); // remove draft account from local cache as we can't tell the kind from draft
        final com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote = draftAccount.getRemote();
        if (Objects.isNull(remote)) {
            return Objects.requireNonNull(get(draftAccount.getName(), draftAccount.getResourceGroupName()), "failed to create account");
        } else {
            final CosmosDBAccount cosmosDBAccount = this.newResource(remote);
            this.addResourceToLocal(cosmosDBAccount.getId(), cosmosDBAccount);
            return cosmosDBAccount;
        }
    }
}
