/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class KeyVersionModule extends AbstractAzResourceModule<KeyVersion, Key, KeyProperties> {
    public static final String NAME = "versions";

    public KeyVersionModule(Key parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, KeyProperties>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient())
            .map(keys -> keys.listPropertiesOfKeyVersions(getParent().getName()).iterableByPage(getPageSize()).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_key_vault.key_vault", params = {"name"})
    protected KeyProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient())
            .map(keys -> keys.getKey(getParent().getName(), name).getProperties())
            .orElse(null);
    }

    @Nonnull
    @Override
    protected KeyVersion newResource(@Nonnull KeyProperties remote) {
        return new KeyVersion(remote, this);
    }

    @Nonnull
    @Override
    protected KeyVersion newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new KeyVersion(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected KeyClient getClient() {
        return this.getParent().getParent().getKeyClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Key Version";
    }
}

