/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.redis.RedisManager;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class RedisCache extends AbstractAzureResource<RedisCache, RedisCacheEntity, com.azure.resourcemanager.redis.models.RedisCache>
    implements AzureOperationEvent.Source<RedisCache>, IAzureResource<RedisCacheEntity> {

    @Nonnull
    private final RedisManager manager;

    public RedisCache(RedisManager manager, com.azure.resourcemanager.redis.models.RedisCache redis) {
        super(new RedisCacheEntity(redis));
        this.manager = manager;
    }

    @AzureOperation(name = "redis.delete", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        manager.redisCaches().deleteById(this.id());
        refresh();
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
}
