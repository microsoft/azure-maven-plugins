/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.redis.RedisManager;
import com.azure.resourcemanager.redis.fluent.RedisClient;
import com.azure.resourcemanager.redis.models.CheckNameAvailabilityParameters;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureRedis extends SubscriptionScoped<AzureRedis> implements AzureService, AzureOperationEvent.Source<AzureRedis> {
    public AzureRedis() {
        super(AzureRedis::new);
    }

    private AzureRedis(@Nonnull final List<Subscription> subscriptions) {
        super(AzureRedis::new, subscriptions);
    }

    @Nonnull
    @AzureOperation(name = "redis.list.subscription|selected", type = AzureOperation.Type.SERVICE)
    public List<RedisCache> list() {
        return getSubscriptions().stream()
            .map(subscription -> RedisManagerFactory.create(subscription.getId()))
            .flatMap(manager -> manager.redisCaches().list().stream())
            .map(redis -> new RedisCache(redis.manager(), redis))
            .collect(Collectors.toList());
    }

    @AzureOperation(name = "redis.get.id", params = {"id"}, type = AzureOperation.Type.SERVICE)
    public RedisCache get(@Nonnull String id) {
        com.azure.resourcemanager.redis.models.RedisCache redisCache =
                RedisManagerFactory.create(ResourceId.fromString(id).subscriptionId()).redisCaches().getById(id);
        return new RedisCache(redisCache.manager(), redisCache);
    }

    @AzureOperation(name = "redis.check_name", params = "name", type = AzureOperation.Type.SERVICE)
    public CheckNameAvailabilityResultEntity checkNameAvailability(String subscriptionId, String name) {
        final RedisManager redisManager = RedisManagerFactory.create(subscriptionId);
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

    public RedisCache create(RedisConfig config) {
        return new Creator(config).commit();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private class Creator implements ICommittable<RedisCache>, AzureOperationEvent.Source<RedisConfig> {

        private RedisConfig config;

        @Override
        @AzureOperation(name = "redis.create", params = {"this.config.getName()"}, type = AzureOperation.Type.SERVICE)
        public RedisCache commit() {
            final com.azure.resourcemanager.redis.models.RedisCache.DefinitionStages.WithSku toCreate =
                RedisManagerFactory.create(config.getSubscription().getId()).redisCaches().define(config.getName())
                .withRegion(config.getRegion().getName())
                .withExistingResourceGroup(config.getResourceGroup().getName());
            com.azure.resourcemanager.redis.models.RedisCache.DefinitionStages.WithCreate withCreate = null;
            if (config.getPricingTier().isBasic()) {
                withCreate = toCreate.withBasicSku(config.getPricingTier().getSize());
            } else if (config.getPricingTier().isStandard()) {
                withCreate = toCreate.withStandardSku(config.getPricingTier().getSize());
            } else if (config.getPricingTier().isPremium()) {
                withCreate = toCreate.withPremiumSku(config.getPricingTier().getSize());
            }
            if (config.isEnableNonSslPort()) {
                withCreate = withCreate.withNonSslPort();
            }
            final com.azure.resourcemanager.redis.models.RedisCache redisCache = withCreate.create();
            return new RedisCache(redisCache.manager(), redisCache);
        }

        public AzureOperationEvent.Source<RedisConfig> getEventSource() {
            return new AzureOperationEvent.Source<RedisConfig>() {};
        }
    }

    @Override
    public String name() {
        return "Redis Caches";
    }
}
