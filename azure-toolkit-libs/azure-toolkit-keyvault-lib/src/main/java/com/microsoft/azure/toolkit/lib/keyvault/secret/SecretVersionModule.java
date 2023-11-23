/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.secret;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
            .map(client -> client.listPropertiesOfSecretVersions(getParent().getName()).collectList().block())
            .map(ItemPage::new)
            .map(IteratorUtils::singletonIterator)
            .orElseGet(IteratorUtils::emptyIterator);
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_secret_version.version", params = {"name"})
    protected SecretProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final SecretAsyncClient client = this.getClient();
        if (Objects.isNull(client)) {
            return null;
        }
        return client.listPropertiesOfSecretVersions(getParent().getName()).toStream()
            .filter(c -> StringUtils.equalsIgnoreCase(c.getVersion(), name))
            .findFirst().orElse(null);
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

    @Nonnull
    @Override
    protected AzResource.Draft<SecretVersion, SecretProperties> newDraftForUpdate(@Nonnull SecretVersion version) {
        return new SecretVersionDraft(version);
    }

    @Nullable
    @Override
    protected SecretAsyncClient getClient() {
        return getParent().getParent().getSecretClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Secret Version";
    }
}

