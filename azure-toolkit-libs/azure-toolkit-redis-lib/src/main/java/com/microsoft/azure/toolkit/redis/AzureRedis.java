/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.redis.RedisManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@Slf4j
public class AzureRedis extends AbstractAzService<RedisServiceSubscription, RedisManager> {
    public AzureRedis() {
        super("Microsoft.Cache");
    }

    @Nonnull
    public RedisCacheModule caches(@Nonnull String subscriptionId) {
        final RedisServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getCacheModule();
    }

    @Nonnull
    @Override
    protected RedisManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return RedisManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(config.getLogLevel()))
            .withPolicy(config.getUserAgentPolicy())
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected RedisServiceSubscription newResource(@Nonnull RedisManager remote) {
        return new RedisServiceSubscription(remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure Cache for Redis";
    }

    public String getServiceNameForTelemetry() {
        return "redis";
    }
}
