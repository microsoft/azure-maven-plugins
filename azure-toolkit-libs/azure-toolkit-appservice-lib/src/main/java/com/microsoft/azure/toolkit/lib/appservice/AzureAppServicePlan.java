/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.microsoft.azure.toolkit.lib.AbstractAzureResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class AzureAppServicePlan extends AbstractAzureResourceModule<AppServicePlan> implements AzureOperationEvent.Source<AzureAppServicePlan> {

    public AzureAppServicePlan() { // for SPI
        super(AzureAppServicePlan::new);
    }

    private AzureAppServicePlan(@Nonnull final List<Subscription> subscriptions) {
        super(AzureAppServicePlan::new, subscriptions);
    }

    @AzureOperation(name = "service.refresh.service", params = "this.name()", type = AzureOperation.Type.SERVICE)
    public void refresh() {
        try {
            CacheManager.evictCache("appservice/{}/plans", CacheEvict.ALL);
            CacheManager.evictCache("appservice/rg/{}/plans", CacheEvict.ALL);
            CacheManager.evictCache("appservice/{}/rg/{}/plan/{}", CacheEvict.ALL);
        } catch (ExecutionException e) {
            log.warn("failed to evict cache", e);
        }
    }

    @Override
    public String name() {
        return "App Service Plan";
    }

    @Override
    @Cacheable(cacheName = "appservice/{}/plans", key = "$subscriptionId", condition = "!(force&&force[0])")
    @AzureOperation(name = "appservice.list_plans.subscription", params = "subscriptionId", type = AzureOperation.Type.SERVICE)
    public List<AppServicePlan> list(@Nonnull String subscriptionId, boolean... force) {
        final AppServiceManager azureResourceManager = getAppServiceManager(subscriptionId);
        return azureResourceManager.appServicePlans().list().stream().parallel()
            .map(appServicePlan -> get(appServicePlan.id()))
            .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservice/rg/{}/plans", key = "$rg", condition = "!(force&&force[0])")
    @AzureOperation(name = "appservice.list_plans.rg", params = "rg", type = AzureOperation.Type.SERVICE)
    public List<AppServicePlan> appServicePlansByResourceGroup(String rg, boolean... force) {
        return getSubscriptions().stream().parallel()
            .map(subscription -> getAppServiceManager(subscription.getId()))
            .flatMap(azureResourceManager -> azureResourceManager.appServicePlans().listByResourceGroup(rg).stream()
                .map(appServicePlan -> new AppServicePlan(appServicePlan, azureResourceManager)))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    @Cacheable(cacheName = "appservice/{}/rg/{}/plan/{}", key = "$subscriptionId/$resourceGroup/$name")
    @AzureOperation(name = "appservice.get_plan.plan|rg", params = {"name", "resourceGroup"}, type = AzureOperation.Type.SERVICE)
    public AppServicePlan get(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        return new AppServicePlan(subscriptionId, resourceGroup, name, getAppServiceManager(subscriptionId));
    }

    // todo: share codes within app service module
    @Cacheable(cacheName = "appservice/{}/manager", key = "$subscriptionId")
    @AzureOperation(name = "appservice.get_client.subscription", params = "subscriptionId", type = AzureOperation.Type.SERVICE)
    AppServiceManager getAppServiceManager(String subscriptionId) {
        return getResourceManager(subscriptionId, AppServiceManager::configure, AppServiceManager.Configurable::authenticate);
    }
}
