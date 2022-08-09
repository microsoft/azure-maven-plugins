/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CosmosDBAccountDraft extends CosmosDBAccount implements
        AzResource.Draft<CosmosDBAccount, com.azure.resourcemanager.cosmos.models.CosmosDBAccount> {

    protected CosmosDBAccountDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @NotNull
    @Override
    public com.azure.resourcemanager.cosmos.models.CosmosDBAccount createResourceInAzure() {
        return null;
    }

    @NotNull
    @Override
    public com.azure.resourcemanager.cosmos.models.CosmosDBAccount updateResourceInAzure(@NotNull com.azure.resourcemanager.cosmos.models.CosmosDBAccount origin) {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Nullable
    @Override
    public CosmosDBAccount getOrigin() {
        return null;
    }

    public static class Config {
        private Subscription subscription;
        private String name;
        private ResourceGroup resourceGroup;
        private Region region;
        private DatabaseAccountKind kind;
    }
}
