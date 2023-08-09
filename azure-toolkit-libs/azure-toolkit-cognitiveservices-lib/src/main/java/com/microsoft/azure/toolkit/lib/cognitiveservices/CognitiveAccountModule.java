/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import com.azure.resourcemanager.cognitiveservices.models.Account;
import com.azure.resourcemanager.cognitiveservices.models.Accounts;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class CognitiveAccountModule extends AbstractAzResourceModule<CognitiveAccount, CognitiveServicesSubscription, Account> {
    public static final String NAME = "accounts";

    public CognitiveAccountModule(@Nonnull CognitiveServicesSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected CognitiveAccount newResource(@Nonnull Account account) {
        return new CognitiveAccount(account, this);
    }

    @Nonnull
    @Override
    protected CognitiveAccount newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new CognitiveAccount(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Account>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient())
            .map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    protected Account loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(this.getClient()).map(c -> c.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteByResourceGroup(id.resourceGroupName(), id.name()));
    }

    @Nullable
    @Override
    protected Accounts getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(CognitiveServicesManager::accounts).orElse(null);
    }
}
