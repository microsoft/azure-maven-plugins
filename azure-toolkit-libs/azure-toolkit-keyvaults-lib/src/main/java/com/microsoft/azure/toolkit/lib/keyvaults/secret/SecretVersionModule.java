/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class SecretVersionModule extends AbstractAzResourceModule<SecretVersion, Secret, SecretProperties> {
    public static final String NAME = "versions";

    public SecretVersionModule(Secret parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, SecretProperties>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient())
            .map(client -> client.listPropertiesOfSecretVersions(getParent().getName()).iterableByPage(getPageSize()).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_key_vault.key_vault", params = {"name"})
    protected SecretProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient())
            .map(client -> client.getSecret(getParent().getName(), name).getProperties()).orElse(null);
    }

    @Nonnull
    @Override
    protected SecretVersion newResource(@Nonnull SecretProperties remote) {
        return new SecretVersion(remote, this);
    }

    @Nonnull
    @Override
    protected SecretVersion newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SecretVersion(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected SecretClient getClient() {
        return getParent().getParent().getSecretClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Secret Version";
    }

}

