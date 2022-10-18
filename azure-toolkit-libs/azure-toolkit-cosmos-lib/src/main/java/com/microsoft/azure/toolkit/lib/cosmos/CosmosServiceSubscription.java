/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.cosmos.CosmosManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
public class CosmosServiceSubscription extends AbstractAzServiceSubscription<CosmosServiceSubscription, CosmosManager> {
    private static final Pattern COSMOS_DB_ACCOUNT_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9\\-]{1,42}[a-z0-9]$");
    private static final String NAME_REQUIREMENT = "The name can contain only lowercase letters, numbers and the '-' character, " +
            "must be between 3 and 44 characters long, and must not start or end with the character '-'.";
    private static final String DUPLICATED_NAME_MESSAGE = "Resource with same name already exists";
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

    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.cosmosDBAccountModule.getResourceTypeName());
    }

    @Nonnull
    public Availability checkNameAvailability(@Nonnull String name) {
        if (!COSMOS_DB_ACCOUNT_NAME_PATTERN.matcher(name).matches()) {
            return new Availability(false, NAME_REQUIREMENT, NAME_REQUIREMENT);
        }
        final boolean exists = Objects.requireNonNull(getRemote()).serviceClient().getDatabaseAccounts().checkNameExists(name);
        final String message = exists ? DUPLICATED_NAME_MESSAGE : null;
        return new Availability(!exists, message, message);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(cosmosDBAccountModule);
    }
}
