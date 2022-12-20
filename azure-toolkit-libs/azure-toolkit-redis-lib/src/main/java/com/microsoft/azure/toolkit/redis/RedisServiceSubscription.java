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
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class RedisServiceSubscription extends AbstractAzServiceSubscription<RedisServiceSubscription, RedisManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final RedisCacheModule cacheModule;

    RedisServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureRedis service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.cacheModule = new RedisCacheModule(this);
    }

    RedisServiceSubscription(@Nonnull RedisManager remote, @Nonnull AzureRedis service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(cacheModule);
    }

    @Nonnull
    public RedisCacheModule caches() {
        return this.cacheModule;
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.cacheModule.getName());
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    @Nonnull
    @AzureOperation(name = "azure/redis.check_name.redis", params = "name")
    public Availability checkNameAvailability(@Nonnull String name) {
        RedisClient redis = Objects.requireNonNull(this.getRemote()).serviceClient().getRedis();
        try {
            redis.checkNameAvailability(new CheckNameAvailabilityParameters().withName(name).withType("Microsoft.Cache/redis"));
            return new Availability(true, null);
        } catch (ManagementException ex) {
            ManagementError value = ex.getValue();
            if (value != null && "NameNotAvailable".equals(value.getCode())) {
                return new Availability(false, String.format("The name '%s' for Redis Cache is not available", name), value.getMessage());
            }
            throw ex;
        }
    }
}

