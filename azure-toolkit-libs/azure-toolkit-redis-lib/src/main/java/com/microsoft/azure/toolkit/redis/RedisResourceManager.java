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
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class RedisResourceManager extends AbstractAzResourceManager<RedisResourceManager, RedisManager> {
    @Nonnull
    private final String subscriptionId;
    private final RedisCacheModule cacheModule;

    RedisResourceManager(@Nonnull String subscriptionId, AzureRedis service) {
        super(subscriptionId, service);
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

    public RedisCacheModule caches() {
        return this.cacheModule;
    }

    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.cacheModule.getName());
    }

    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    @AzureOperation(name = "redis.check_name.redis", params = "name", type = AzureOperation.Type.SERVICE)
    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String name) {
        RedisClient redis = Objects.requireNonNull(this.getRemote()).serviceClient().getRedis();
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

