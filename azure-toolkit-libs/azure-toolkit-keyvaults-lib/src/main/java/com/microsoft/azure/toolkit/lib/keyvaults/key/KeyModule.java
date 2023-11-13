/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class KeyModule extends AbstractAzResourceModule<Key, KeyVault, KeyProperties> {
    public static final String NAME = "keys";

    public KeyModule(KeyVault parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, KeyProperties>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient())
            .map(c -> c.listPropertiesOfKeys().iterableByPage(getPageSize()).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_key_vault.key_vault", params = {"name"})
    protected KeyProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(c -> c.getKey(name).getProperties()).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/keyvaults.delete_key_vault.key_vault", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(this.getClient())
            .ifPresent(client -> client.beginDeleteKey(id.name()).waitForCompletion().getValue());
    }

    @Nonnull
    @Override
    protected Key newResource(@Nonnull KeyProperties remote) {
        return new Key(remote, this);
    }

    @Nonnull
    @Override
    protected Key newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Key(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected KeyClient getClient() {
        return this.getParent().getKeyClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Key";
    }
}
