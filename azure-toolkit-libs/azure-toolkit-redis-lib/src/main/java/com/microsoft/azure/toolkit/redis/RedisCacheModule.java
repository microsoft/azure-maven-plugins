/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.resourcemanager.redis.RedisManager;
import com.azure.resourcemanager.redis.models.RedisCaches;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

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

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected RedisCacheDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new RedisCacheDraft(name, resourceGroupName, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected RedisCacheDraft newDraftForUpdate(@Nonnull RedisCache origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new RedisCacheDraft(origin);
    }

    @Nonnull
    protected RedisCache newResource(@Nonnull com.azure.resourcemanager.redis.models.RedisCache r) {
        return new RedisCache(r, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Redis cache";
    }
}
