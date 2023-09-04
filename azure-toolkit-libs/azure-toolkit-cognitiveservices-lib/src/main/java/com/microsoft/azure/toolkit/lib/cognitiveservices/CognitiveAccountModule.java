/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import com.azure.resourcemanager.cognitiveservices.models.Account;
import com.azure.resourcemanager.cognitiveservices.models.Accounts;
import com.azure.resourcemanager.cognitiveservices.models.ResourceSku;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
    @AzureOperation(name = "azure/openai.load_account.account", params = {"name"})
    protected Account loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(this.getClient()).map(c -> c.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/openai.delete_account.account", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteByResourceGroup(id.resourceGroupName(), id.name()));
    }

    @Nullable
    @Override
    protected Accounts getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(CognitiveServicesManager::accounts).orElse(null);
    }

    @NotNull
    @Override
    protected CognitiveAccountDraft newDraftForCreate(@NotNull String name, @Nullable String rgName) {
        assert rgName != null : "'Resource group' is required.";
        return new CognitiveAccountDraft(name, rgName, this);
    }

    public List<AccountModel> listModels(@Nonnull final Region region) {
        return Optional.ofNullable(getParent().getRemote())
            .map(CognitiveServicesManager::models)
            .map(models -> models.list(region.getName()).stream().map(AccountModel::fromModel).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    @Nonnull
    public List<AccountSku> listSkus(@Nullable final Region region) {
        final List<ResourceSku> resourceSkus = listCognitiveAccountSku();
        return resourceSkus.stream()
            .filter(sku -> Objects.isNull(region) || sku.locations().stream().anyMatch(item -> StringUtils.equalsIgnoreCase(region.getName(), item)))
            .map(AccountSku::fromSku).distinct().collect(Collectors.toList());
    }

    @Nonnull
    public List<Region> listRegion(@Nullable AccountSku sku) {
        final List<ResourceSku> resourceSkus = listCognitiveAccountSku();
        return resourceSkus.stream()
            .filter(s -> Objects.isNull(sku) || (StringUtils.equalsIgnoreCase(sku.getName(), s.name()) && StringUtils.equalsIgnoreCase(sku.getTier(), s.tier())))
            .flatMap(s -> s.locations().stream())
            .map(Region::fromName).distinct().collect(Collectors.toList());
    }

    @Cacheable(cacheName = "openAI/subscriptions/{}/sku", key = "${this.getSubscriptionId()}")
    private List<ResourceSku> listCognitiveAccountSku() {
        this.getSubscriptionId();
        final CognitiveServicesManager remote = getParent().getRemote();
        if (Objects.isNull(remote)) {
            return Collections.emptyList();
        }
        return remote.resourceSkus().list().stream()
            .filter(s -> StringUtils.equalsIgnoreCase(s.kind(), "OpenAI") && StringUtils.equalsIgnoreCase(s.resourceType(), "accounts"))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure OpenAI service";
    }
}
