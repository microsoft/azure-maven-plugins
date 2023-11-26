/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.keyvault.KeyVaultManager;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.keyvault.models.Vaults;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class KeyVaultModule extends AbstractAzResourceModule<KeyVault, KeyVaultSubscription, Vault> {
    public static final String NAME = "vaults";

    public KeyVaultModule(KeyVaultSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Vault>> loadResourcePagesFromAzure() {
        final Vaults client = getClient();
        final KeyVaultManager manager = this.parent.getRemote();
        if (Objects.isNull(client) || Objects.isNull(manager)) {
            return Collections.emptyIterator();
        }
        return manager.serviceClient().getVaults().list().streamByPage(getPageSize())
            .map(p -> new ItemPage<>(p.getValue().stream()
                .map(r -> ResourceId.fromString(r.id()))
                .parallel()
                .map(id -> loadResourceFromAzure(id.name(), id.resourceGroupName()))))
            .iterator();
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvault.load_key_vault.key_vault", params = {"name"})
    protected Vault loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(vaults -> vaults.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/keyvault.delete_key_vault.key_vault", params = {"nameFromResourceId(resourceId)"})
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
        return "Key Vault";
    }
}

