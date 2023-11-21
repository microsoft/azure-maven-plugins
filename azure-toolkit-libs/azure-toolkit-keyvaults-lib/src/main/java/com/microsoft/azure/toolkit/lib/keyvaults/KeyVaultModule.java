/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.keyvault.KeyVaultManager;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.keyvault.models.Vaults;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeyVaultModule extends AbstractAzResourceModule<KeyVault, KeyVaultSubscription, Vault> {
    public static final String NAME = "vaults";

    public KeyVaultModule(KeyVaultSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Vault>> loadResourcePagesFromAzure() {
        final Vaults client = getClient();
        if (Objects.isNull(client)) {
            return Collections.emptyIterator();
        }
        final ResourceGroupModule groups = Azure.az(AzureResources.class).groups(getSubscriptionId());
        final List<Vault> results = groups.list().stream()
            .flatMap(g -> client.listByResourceGroup(g.getResourceGroupName()).stream())
            .collect(Collectors.toList());
        return Collections.singleton(new ItemPage<>(results)).iterator();
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_key_vault.key_vault", params = {"name"})
    protected Vault loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(vaults -> vaults.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/keyvaults.delete_key_vault.key_vault", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(vaults -> vaults.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected KeyVault newResource(@Nonnull Vault remote) {
        return new KeyVault(remote, this);
    }

    @Nonnull
    @Override
    protected KeyVault newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new KeyVault(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @NotNull
    @Override
    protected AzResource.Draft<KeyVault, Vault> newDraftForCreate(@NotNull String name, @org.jetbrains.annotations.Nullable String rgName) {
        return new KeyVaultDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nullable
    @Override
    protected Vaults getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(KeyVaultManager::vaults).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Key vault";
    }
}

