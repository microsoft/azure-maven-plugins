/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.redis.RedisManager;
import com.azure.resourcemanager.redis.fluent.RedisClient;
import com.azure.resourcemanager.redis.models.CheckNameAvailabilityParameters;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class AzureRedis extends AbstractAzResourceModule<RedisResourceManager, AzResource.None, RedisManager> implements AzService {
    public AzureRedis() {
        super("Microsoft.Cache", AzResource.NONE);
    }

    @Nonnull
    public RedisCacheModule caches(@Nonnull String subscriptionId) {
        final RedisResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getCacheModule();
    }

    public RedisResourceManager forSubscription(@Nonnull String subscriptionId) {
        return this.get(subscriptionId, null);
    }

    @Nonnull
    protected Stream<RedisManager> loadResourcesFromAzure() {
        return Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream().parallel()
            .map(Subscription::getId).map(i -> loadResourceFromAzure(i, null));
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
    protected Object getClient() {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/%s", resourceName, rg, this.getName());
    }

    @AzureOperation(name = "redis.check_name.redis", params = "name", type = AzureOperation.Type.SERVICE)
    public CheckNameAvailabilityResultEntity checkNameAvailability(String subscriptionId, String name) {
        final RedisManager redisManager = loadResourceFromAzure(subscriptionId, null);
        RedisClient redis = redisManager.redisCaches().manager().serviceClient().getRedis();
        try {
            redis.checkNameAvailability(new CheckNameAvailabilityParameters().withName(name).withType("Microsoft.Cache/redis"));
            return new CheckNameAvailabilityResultEntity(true, null);
        } catch (ManagementException ex) {
            ManagementError value = ex.getValue();
            if (value != null && "NameNotAvailable".equals(value.getCode())) {
                return new CheckNameAvailabilityResultEntity(false, String.format("The name '%s' for Redis Cache is not available", name), value.getMessage());
            }
            throw ex;
        }
    }
}
