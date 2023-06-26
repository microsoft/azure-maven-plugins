/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.redis.model.PricingTier;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RedisCache extends AbstractAzResource<RedisCache, RedisServiceSubscription, com.azure.resourcemanager.redis.models.RedisCache>
    implements Deletable {
    private static final int JEDIS_TIMEOUT = 500;

    private JedisPool jedisPool;

    protected RedisCache(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull RedisCacheModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected RedisCache(@Nonnull RedisCache origin) {
        super(origin);
        this.jedisPool = origin.jedisPool;
    }

    protected RedisCache(@Nonnull com.azure.resourcemanager.redis.models.RedisCache remote, @Nonnull RedisCacheModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
    }

    @Override
    public void delete() {
        if (Objects.nonNull(this.jedisPool) && !this.jedisPool.isClosed()) {
            try {
                this.jedisPool.close();
            } catch (Exception e) {
                final String message = String.format("Failed to close jedis pool of Redis Cache(%s)", this.getName());
                AzureMessager.getMessager().warning(message);
            }
        }
        super.delete();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull com.azure.resourcemanager.redis.models.RedisCache remote) {
        return remote.innerModel().provisioningState().toString();
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    public PricingTier getPricingTier() {
        return remoteOptional().map(com.azure.resourcemanager.redis.models.RedisCache::sku).map(PricingTier::from).orElse(null);
    }

    @Nullable
    public String getType() {
        return remoteOptional().map(Resource::type).orElse(null);
    }

    public int getSSLPort() {
        return remoteOptional().map(com.azure.resourcemanager.redis.models.RedisCache::sslPort).orElse(-1);
    }

    public boolean isNonSslPortEnabled() {
        return remoteOptional().map(com.azure.resourcemanager.redis.models.RedisCache::nonSslPort).orElse(false);
    }

    @Nullable
    public String getRedisVersion() {
        return remoteOptional().map(com.azure.resourcemanager.redis.models.RedisCache::redisVersion).orElse(null);
    }

    @Nullable
    public String getPrimaryKey() {
        return remoteOptional().map(remote -> remote.keys().primaryKey()).orElse(null);
    }

    @Nullable
    public String getSecondaryKey() {
        return remoteOptional().map(remote -> remote.keys().secondaryKey()).orElse(null);
    }

    @Nullable
    public String getHostName() {
        return remoteOptional().map(com.azure.resourcemanager.redis.models.RedisCache::hostname).orElse(null);
    }

    @Nonnull
    @AzureOperation(name = "redis.get_jedis_pool.redis", params = {"this.getName()"})
    public synchronized JedisPool getJedisPool() {
        if (Objects.isNull(this.jedisPool) || this.jedisPool.isClosed()) {
            final String hostName = this.getHostName();
            final String password = this.getPrimaryKey();
            final int port = this.getSSLPort();
            this.jedisPool = new JedisPool(new JedisPoolConfig(), hostName, port, JEDIS_TIMEOUT, password, true);
        }
        return this.jedisPool;
    }
}
