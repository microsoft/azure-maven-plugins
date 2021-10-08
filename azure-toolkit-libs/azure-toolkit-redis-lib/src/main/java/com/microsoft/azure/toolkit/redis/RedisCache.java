/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.redis.RedisManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.redis.model.RedisCacheEntity;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Nonnull;
import java.util.Objects;

public class RedisCache extends AbstractAzureResource<RedisCache, RedisCacheEntity, com.azure.resourcemanager.redis.models.RedisCache>
        implements Removable, AzureOperationEvent.Source<RedisCache>, IAzureResource<RedisCacheEntity> {
    private static final int JEDIS_TIMEOUT = 500;

    @Nonnull
    private final RedisManager manager;
    private JedisPool jedisPool;

    public RedisCache(com.azure.resourcemanager.redis.models.RedisCache redis) {
        super(new RedisCacheEntity(redis));
        this.manager = redis.manager();
    }

    @AzureOperation(name = "redis.delete", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        if (this.exists()) {
            this.status(Status.PENDING);
            manager.redisCaches().deleteById(this.id());
            Azure.az(AzureRedis.class).refresh();
        }
    }

    @AzureOperation(name = "redis.get_jedis_pool", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public synchronized JedisPool getJedisPool() {
        if (Objects.isNull(this.jedisPool)) {
            final RedisCacheEntity entity = this.entity();
            final String hostName = entity.getHostName();
            final String password = entity.getPrimaryKey();
            final int port = entity.getSSLPort();
            this.jedisPool = new JedisPool(new JedisPoolConfig(), hostName, port, JEDIS_TIMEOUT, password, true);
        }
        return this.jedisPool;
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.redis.models.RedisCache loadRemote() {
        try {
            this.entity().setRemote(manager.redisCaches().getById(this.entity.getId()));
        } catch (ManagementException ex) {
            if (HttpStatus.SC_NOT_FOUND == ex.getResponse().getStatusCode()) {
                return null;
            } else {
                throw ex;
            }
        }
        return entity.getRemote();
    }

    @Override
    public void remove() {
        this.delete();
    }
}
