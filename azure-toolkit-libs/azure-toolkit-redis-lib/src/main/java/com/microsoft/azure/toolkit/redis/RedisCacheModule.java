/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.resourcemanager.redis.RedisManager;
import com.azure.resourcemanager.redis.models.RedisCaches;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Optional;

public class RedisCacheModule extends AbstractAzResourceModule<RedisCache, RedisResourceManager, com.azure.resourcemanager.redis.models.RedisCache> {

    public static final String NAME = "Redis";

    public RedisCacheModule(@Nonnull RedisResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public RedisCaches getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(RedisManager::redisCaches).orElse(null);
    }

    @Nonnull
    protected RedisCache newResource(@Nonnull com.azure.resourcemanager.redis.models.RedisCache r) {
        return new RedisCache(r, this);
    }

    @Override
    protected RedisCacheDraft newDraft(@Nonnull String name, String resourceGroup) {
        return new RedisCacheDraft(name, resourceGroup, this);
    }
}
