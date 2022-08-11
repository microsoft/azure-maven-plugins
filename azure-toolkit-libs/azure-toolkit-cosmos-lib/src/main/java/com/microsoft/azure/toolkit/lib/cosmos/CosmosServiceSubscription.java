/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.cosmos.CosmosManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class CosmosServiceSubscription extends AbstractAzServiceSubscription<CosmosServiceSubscription, CosmosManager> {
    @Nonnull
    private final String subscriptionId;
    private final CosmosDBAccountModule cosmosDBAccountModule;

    protected CosmosServiceSubscription(@NotNull String subscriptionId, @NotNull AzureCosmosService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.cosmosDBAccountModule = new CosmosDBAccountModule(this);
    }

    protected CosmosServiceSubscription(@Nonnull CosmosManager manager, @Nonnull AzureCosmosService service) {
        this(manager.subscriptionId(), service);
    }

    public CosmosDBAccountModule databaseAccounts() {
        return this.cosmosDBAccountModule;
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, CosmosServiceSubscription, ?>> getSubModules() {
        return Collections.singletonList(cosmosDBAccountModule);
    }
}
