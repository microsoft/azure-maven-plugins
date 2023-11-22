/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SecretModule extends AbstractAzResourceModule<Secret, KeyVault, SecretProperties> {
    public static final String NAME = "secrets";

    public SecretModule(KeyVault parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, SecretProperties>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient())
            .map(c -> c.listPropertiesOfSecrets().toStream().filter(p -> BooleanUtils.isNotTrue(p.isManaged())).collect(Collectors.toList()))
            .map(ItemPage::new)
            .map(IteratorUtils::singletonIterator)
            .orElseGet(IteratorUtils::emptyIterator);
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_secret.secret", params = {"name"})
    protected SecretProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final SecretAsyncClient client = this.getClient();
        if (Objects.isNull(client)) {
            return null;
        }
        return client.listPropertiesOfSecrets().toStream()
            .filter(c -> StringUtils.equalsIgnoreCase(c.getName(), name))
            .findFirst().orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/keyvaults.delete_secret.secret", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(this.getClient()).ifPresent(vaults -> vaults.beginDeleteSecret(id.name()).blockLast());
    }

    @Nonnull
    @Override
    protected Secret newResource(@Nonnull SecretProperties remote) {
        return new Secret(remote, this);
    }

    @Nonnull
    @Override
    protected Secret newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Secret(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected SecretAsyncClient getClient() {
        return getParent().getSecretClient();
    }

    @Nonnull
    @Override
    protected SecretDraft newDraftForCreate(@Nonnull String name, @org.jetbrains.annotations.Nullable String rgName) {
        return new SecretDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Secret";
    }
}

