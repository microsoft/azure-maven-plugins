/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.resourcemanager.redis.RedisManager;
import com.azure.resourcemanager.redis.models.RedisCaches;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class RedisCacheModule extends AbstractAzResourceModule<RedisCache, RedisServiceSubscription, com.azure.resourcemanager.redis.models.RedisCache> {

    public static final String NAME = "Redis";

    public RedisCacheModule(@Nonnull RedisServiceSubscription parent) {
        super(NAME, parent);
    }

    @Override
    public RedisCaches getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(RedisManager::redisCaches).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected RedisCacheDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new RedisCacheDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected RedisCacheDraft newDraftForUpdate(@Nonnull RedisCache origin) {
        return new RedisCacheDraft(origin);
    }

    @Nonnull
    protected RedisCache newResource(@Nonnull com.azure.resourcemanager.redis.models.RedisCache r) {
        return new RedisCache(r, this);
    }

    @Nonnull
    protected RedisCache newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new RedisCache(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Redis cache";
    }
}
