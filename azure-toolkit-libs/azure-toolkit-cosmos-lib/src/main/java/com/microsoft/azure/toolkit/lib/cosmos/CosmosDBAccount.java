/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountConnectionStrings;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKeys;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CosmosDBAccount extends AbstractAzResource<CosmosDBAccount, CosmosServiceSubscription,
        com.azure.resourcemanager.cosmos.models.CosmosDBAccount> implements Deletable {

    protected CosmosDBAccount(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
    }

    protected CosmosDBAccount(@Nonnull CosmosDBAccount account) {
        super(account);
    }

    protected CosmosDBAccount(@Nonnull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote, @Nonnull CosmosDBAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Nullable
    public DatabaseAccountKeys listKeys() {
        return Optional.ofNullable(getRemote())
                .map(remote -> DatabaseAccountKeys.fromDatabaseAccountListKeysResult(remote.listKeys())).orElse(null);
    }

    @Nullable
    public DatabaseAccountConnectionStrings listConnectionStrings() {
        return Optional.ofNullable(getRemote())
                .map(remote -> DatabaseAccountConnectionStrings.fromDatabaseAccountListConnectionStringsResult(remote.listConnectionStrings(), Objects.requireNonNull(getKind())))
                .orElse(null);
    }

    @Nullable
    public DatabaseAccountKind getKind() {
        return Optional.ofNullable(getRemote()).map(DatabaseAccountKind::fromAccount).orElse(null);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, CosmosDBAccount, ?>> getSubModules() {
        return Collections.emptyList();
    }

    public String getDocumentEndpoint() {
        return getRemote().documentEndpoint();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote) {
        // todo: investigate how to get status instead of provisioning state
        return remote.innerModel().provisioningState();
    }
}
