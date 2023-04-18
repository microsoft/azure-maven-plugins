/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.cosmos.model.CosmosDBAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountConnectionStrings;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKeys;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CosmosDBAccount extends AbstractAzResource<CosmosDBAccount, CosmosServiceSubscription,
        com.azure.resourcemanager.cosmos.models.CosmosDBAccount> implements Deletable {

    private DatabaseAccountKeys databaseAccountKeys;
    private DatabaseAccountConnectionStrings databaseAccountConnectionStrings;

    protected CosmosDBAccount(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
    }

    protected CosmosDBAccount(@Nonnull CosmosDBAccount account) {
        super(account);
        this.databaseAccountKeys = account.databaseAccountKeys;
        this.databaseAccountConnectionStrings = account.databaseAccountConnectionStrings;
    }

    protected CosmosDBAccount(@Nonnull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote, @Nonnull CosmosDBAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Nonnull
    public DatabaseAccountKeys listKeys() {
        return remoteOptional(true).map(ignore -> this.databaseAccountKeys).orElseGet(DatabaseAccountKeys::new);
    }

    @Nonnull
    public DatabaseAccountConnectionStrings listConnectionStrings() {
        return remoteOptional(true).map(ignore -> this.databaseAccountConnectionStrings).orElseGet(DatabaseAccountConnectionStrings::new);
    }

    @Nullable
    public DatabaseAccountKind getKind() {
        return Optional.ofNullable(getRemote()).map(DatabaseAccountKind::fromAccount).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nullable
    public String getDocumentEndpoint() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.documentEndpoint()).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    public CosmosDBAccountConnectionString getCosmosDBAccountPrimaryConnectionString() {
        return null;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote) {
        // todo: investigate how to get status instead of provisioning state
        return remote.innerModel().provisioningState();
    }

    @Override
    protected void updateAdditionalProperties(com.azure.resourcemanager.cosmos.models.CosmosDBAccount newRemote, com.azure.resourcemanager.cosmos.models.CosmosDBAccount oldRemote) {
        if (newRemote == null) {
            this.databaseAccountKeys = null;
            this.databaseAccountConnectionStrings = null;
        } else {
            this.databaseAccountKeys = DatabaseAccountKeys.fromDatabaseAccountListKeysResult(newRemote.listKeys());
            this.databaseAccountConnectionStrings = DatabaseAccountConnectionStrings.fromDatabaseAccountListConnectionStringsResult(newRemote.listConnectionStrings(), Objects.requireNonNull(this.getKind()));
        }
    }
}
