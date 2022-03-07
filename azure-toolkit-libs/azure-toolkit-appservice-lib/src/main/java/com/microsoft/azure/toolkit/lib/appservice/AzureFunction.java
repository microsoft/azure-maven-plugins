/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.microsoft.azure.toolkit.lib.AbstractAzureResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class AzureFunction extends AbstractAzureResourceModule<FunctionApp> implements AzureOperationEvent.Source<AzureFunction> {
    public AzureFunction() { // for SPI
        super(AzureFunction::new);
    }

    private AzureFunction(@Nonnull final List<Subscription> subscriptions) {
        super(AzureFunction::new, subscriptions);
    }

    @Override
    @Cacheable(cacheName = "appservice/{}/functionapps", key = "$sid", condition = "!(force&&force[0])")
    @AzureOperation(name = "functionapp.list_apps.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    public List<FunctionApp> list(@Nonnull String sid, boolean... force) {
        final AppServiceManager azureResourceManager = getAppServiceManager(sid);
        return azureResourceManager
            .functionApps().list().stream().parallel()
            .filter(functionAppBasic -> StringUtils.containsIgnoreCase(functionAppBasic.innerModel().kind(), "functionapp")) // Filter out function apps
            .map(functionAppBasic -> get(functionAppBasic.id()))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    @Cacheable(cacheName = "appservice/{}/rg/{}/functionapp/{}", key = "$sid/$rg/$name")
    @AzureOperation(name = "functionapp.get_app.app|rg", params = {"name", "rg"}, type = AzureOperation.Type.SERVICE)
    public FunctionApp get(@Nonnull String sid, @Nonnull String rg, @Nonnull String name) {
        return new FunctionApp(sid, rg, name, getAppServiceManager(sid));
    }

    // todo: share codes within app service module
    @Cacheable(cacheName = "appservice/{}/manager", key = "$subscriptionId")
    @AzureOperation(name = "appservice.get_client.subscription", params = "subscriptionId", type = AzureOperation.Type.SERVICE)
    AppServiceManager getAppServiceManager(String subscriptionId) {
        return getResourceManager(subscriptionId, AppServiceManager::configure, AppServiceManager.Configurable::authenticate);
    }

    @AzureOperation(name = "service.refresh.service", params = "this.name()", type = AzureOperation.Type.SERVICE)
    public void refresh() {
        try {
            CacheManager.evictCache("appservice/{}/functionapps", CacheEvict.ALL);
            CacheManager.evictCache("appservice/{}/rg/{}/functionapp/{}", CacheEvict.ALL);
        } catch (ExecutionException e) {
            log.warn("failed to evict cache", e);
        }
    }

    @Override
    public String name() {
        return "Azure Function";
    }
}
