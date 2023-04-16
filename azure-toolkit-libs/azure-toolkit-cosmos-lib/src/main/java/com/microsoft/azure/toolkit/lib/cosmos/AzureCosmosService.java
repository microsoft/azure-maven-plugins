/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.cosmos.CosmosManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureCosmosService extends AbstractAzService<CosmosServiceSubscription, CosmosManager> {

    public AzureCosmosService() {
        super("Microsoft.DocumentDB");
    }

    @Nonnull
    @Override
    protected CosmosServiceSubscription newResource(@Nonnull CosmosManager cosmosManager) {
        return new CosmosServiceSubscription(cosmosManager.subscriptionId(), this);
    }

    @Nonnull
    public CosmosDBAccountModule databaseAccounts(@Nonnull String subscriptionId) {
        final CosmosServiceSubscription serviceSubscription = get(subscriptionId, null);
        assert serviceSubscription != null;
        return serviceSubscription.databaseAccounts();
    }

    @Nonnull
    public List<CosmosDBAccount> getDatabaseAccounts() {
        return this.list().stream().flatMap(m -> m.databaseAccounts().list().stream()).collect(Collectors.toList());
    }

    @Nonnull
    public List<CosmosDBAccount> getDatabaseAccounts(@Nonnull DatabaseAccountKind kind) {
        return this.list().stream().flatMap(m -> m.databaseAccounts().list().stream().filter(a -> kind.equals(a.getKind()))).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure Cosmos DB";
    }

    @Nullable
    @Override
    protected CosmosManager loadResourceFromAzure(@Nonnull String subscriptionId, @Nullable String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return CosmosManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }
}
