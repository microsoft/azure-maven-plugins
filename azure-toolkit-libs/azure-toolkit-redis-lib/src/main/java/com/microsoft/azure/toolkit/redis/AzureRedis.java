/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.redis.RedisManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.util.Optional;

@Log4j2
public class AzureRedis extends AbstractAzService<RedisResourceManager, RedisManager> {
    public AzureRedis() {
        super("Microsoft.Cache");
    }

    @Nonnull
    public RedisCacheModule caches(@Nonnull String subscriptionId) {
        final RedisResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getCacheModule();
    }

    @Nonnull
    @Override
    protected RedisManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return RedisManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
            .withLogLevel(logLevel)
            .withPolicy(AzureService.getUserAgentPolicy(userAgent)) // set user agent with policy
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Override
    protected RedisResourceManager newResource(@Nonnull RedisManager remote) {
        return new RedisResourceManager(remote, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Azure Cache for Redis";
    }
}
