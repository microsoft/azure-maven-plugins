/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis.model;

import com.azure.resourcemanager.redis.models.RedisCache;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import java.util.Optional;

public class RedisCacheEntity extends AbstractAzureResource.RemoteAwareResourceEntity<com.azure.resourcemanager.redis.models.RedisCache> {

    @Nonnull
    private final ResourceId resourceId;

    public RedisCacheEntity(com.azure.resourcemanager.redis.models.RedisCache redis) {
        this.resourceId = ResourceId.fromString(redis.id());
        this.remote = redis;
    }

    @Override
    public String getSubscriptionId() {
        return resourceId.subscriptionId();
    }

    public String getId() {
        return resourceId.id();
    }

    @Override
    public String getName() {
        return resourceId.name();
    }

    public String getResourceGroupName() {
        return resourceId.resourceGroupName();
    }

    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    public String getType() {
        return remoteOptional().map(Resource::type).orElse(null);
    }

    public int getSSLPort() {
        return remoteOptional().map(RedisCache::sslPort).orElse(-1);
    }

    public boolean getNonSslPortEnabled() {
        return remoteOptional().map(RedisCache::nonSslPort).orElse(false);
    }

    public String getRedisVersion() {
        return remoteOptional().map(RedisCache::redisVersion).orElse(null);
    }

    public String getPrimaryKey() {
        return remoteOptional().map(remote -> remote.keys().primaryKey()).orElse(null);
    }

    public String getSecondaryKey() {
        return remoteOptional().map(remote -> remote.keys().secondaryKey()).orElse(null);
    }

    public String getHostName() {
        return remoteOptional().map(RedisCache::hostname).orElse(null);
    }

    private Optional<com.azure.resourcemanager.redis.models.RedisCache> remoteOptional() {
        return Optional.ofNullable(this.remote);
    }
}
