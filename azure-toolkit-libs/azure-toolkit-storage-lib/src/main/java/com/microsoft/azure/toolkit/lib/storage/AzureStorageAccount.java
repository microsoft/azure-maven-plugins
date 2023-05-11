/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.storage.StorageManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AzureStorageAccount extends AbstractAzService<StorageServiceSubscription, StorageManager> {

    public AzureStorageAccount() {
        super("Microsoft.Storage");
    }

    @Nonnull
    public StorageAccountModule accounts(@Nonnull String subscriptionId) {
        final StorageServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getStorageModule();
    }

    @Nullable
    public StorageAccount account(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final StorageServiceSubscription rm = get(id.subscriptionId(), null);
        assert rm != null;
        return rm.getStorageModule().get(resourceId);
    }

    @Nonnull
    public List<StorageAccount> accounts() {
        final List<StorageAccount> result = new ArrayList<>();
        result.add(AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT);
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            final List<StorageAccount> collect = this.list().stream().flatMap(m -> m.storageAccounts().list().stream()).collect(Collectors.toList());
            result.addAll(collect);
        }
        return result;
    }

    @Nonnull
    @Override
    protected StorageManager loadResourceFromAzure(@Nonnull String subscriptionId, @Nullable String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return StorageManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogLevel(logLevel)
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent)) // set user agent with policy
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected StorageServiceSubscription newResource(@Nonnull StorageManager remote) {
        return new StorageServiceSubscription(remote, this);
    }

    @Nonnull
    public List<Performance> listSupportedPerformances() {
        return Performance.values();
    }

    @Nonnull
    public List<Kind> listSupportedKinds(@Nonnull Performance performance) {
        return Kind.values().stream().filter(k -> Objects.equals(k.getPerformance(), performance)).collect(Collectors.toList());
    }

    @Nonnull
    public List<Redundancy> listSupportedRedundancies(@Nonnull Performance performance, @Nullable Kind kind) {
        return Redundancy.values().stream()
            .filter(r -> Objects.equals(r.getPerformance(), performance))
            .filter(r -> !(Objects.equals(Kind.PAGE_BLOB_STORAGE, kind) && Objects.equals(r, Redundancy.PREMIUM_ZRS)))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Storage accounts";
    }
}
