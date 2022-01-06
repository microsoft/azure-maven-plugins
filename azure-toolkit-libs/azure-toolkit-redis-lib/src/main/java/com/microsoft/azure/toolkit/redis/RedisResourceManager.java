/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.azure.resourcemanager.redis.RedisManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.Manager;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.IResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class RedisResourceManager extends AbstractAzResource<RedisResourceManager, AzResource.None, RedisManager>
    implements IResourceManager {
    @Nonnull
    private final String subscriptionId;
    private final RedisCacheModule cacheModule;

    RedisResourceManager(@Nonnull String subscriptionId, AzureRedis service) {
        super(subscriptionId, AzResource.RESOURCE_GROUP_PLACEHOLDER, service);
        this.subscriptionId = subscriptionId;
        this.cacheModule = new RedisCacheModule(this);
    }

    RedisResourceManager(@Nonnull RedisManager remote, AzureRedis service) {
        this(remote.subscriptionId(), service);
    }

    @Override
    public List<AzResourceModule<?, RedisResourceManager, ?>> getSubModules() {
        return Collections.singletonList(cacheModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull RedisManager remote) {
        return Status.UNKNOWN;
    }

    public RedisCacheModule caches() {
        return this.cacheModule;
    }

    public List<Region> listSupportedRegions() {
        return IResourceManager.super.listSupportedRegions(this.cacheModule.getName());
    }

    @Override
    public AzService getService() {
        return (AzureRedis) this.getModule();
    }

    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }
}

