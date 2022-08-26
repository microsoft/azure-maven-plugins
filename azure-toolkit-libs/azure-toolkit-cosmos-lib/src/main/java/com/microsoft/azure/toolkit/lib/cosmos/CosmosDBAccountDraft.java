/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.cosmos.CosmosManager;
import com.azure.resourcemanager.cosmos.models.CosmosDBAccount.DefinitionStages.WithConsistencyPolicy;
import com.azure.resourcemanager.cosmos.models.CosmosDBAccount.DefinitionStages.WithKind;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

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
        AzureMessager.getMessager().info(AzureString.format("Start creating account({0})...", this.getName()));
        final com.azure.resourcemanager.cosmos.models.CosmosDBAccount account = withConsistencyPolicy.withSessionConsistency()
                .withWriteReplication(com.azure.core.management.Region.fromName(region.getName()))
                .create();
        AzureMessager.getMessager().success(AzureString.format("Account({0}) is successfully created.", this.getName()));
        return account;
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

        public static Config getDefaultConfig(final ResourceGroup resourceGroup) {
            final List<Subscription> subs = az(AzureAccount.class).account().getSelectedSubscriptions();
            Preconditions.checkArgument(CollectionUtils.isNotEmpty(subs), "There are no subscriptions in your account.");
            final String name = String.format("cosmos-db-%s", Utils.getTimestamp());
            final String defaultResourceGroupName = String.format("rg-%s", name);
            final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
            final Region historyReg = CacheManager.getUsageHistory(Region.class).peek();
            final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
                    .peek(r -> Objects.isNull(historySub) || r.getSubscriptionId().equals(historySub.getId()));
            final ResourceGroup group = Optional.ofNullable(resourceGroup)
                    .orElseGet(() -> Optional.ofNullable(historyRg).orElseGet(() -> az(AzureResources.class)
                            .groups(subs.get(0).getId()).create(defaultResourceGroupName, defaultResourceGroupName)));
            final Subscription subscription = Optional.of(group).map(AzResource::getSubscription)
                    .orElseGet(() -> Optional.ofNullable(historySub).orElseGet(() -> subs.get(0)));
            final CosmosDBAccountDraft.Config config = new CosmosDBAccountDraft.Config();
            config.setName(name);
            config.setSubscription(subscription);
            config.setResourceGroup(group);
            config.setRegion(historyReg);
            config.setKind(DatabaseAccountKind.SQL);
            return config;
        }
    }
}
