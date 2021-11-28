/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.microsoft.azure.toolkit.lib.AbstractAzureResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class AzureWebApp extends AbstractAzureResourceModule<IWebApp> implements AzureOperationEvent.Source<AzureWebApp> {
    public AzureWebApp() { // for SPI
        super(AzureWebApp::new);
    }

    private AzureWebApp(@Nonnull final List<Subscription> subscriptions) {
        super(AzureWebApp::new, subscriptions);
    }

    @Override
    @Cacheable(cacheName = "appservice/{}/webapps", key = "$sid", condition = "!(force&&force[0])")
    @AzureOperation(name = "webapp.list.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    public List<IWebApp> list(@NotNull String sid, boolean... force) {
        final AppServiceManager azureResourceManager = getAppServiceManager(sid);
        return azureResourceManager.webApps().list().stream().parallel()
                .filter(webAppBasic -> !StringUtils.containsIgnoreCase(webAppBasic.innerModel().kind(), "functionapp")) // Filter out function apps
                .map(webAppBasic -> new WebApp(webAppBasic, azureResourceManager))
                .collect(Collectors.toList());
    }

    @NotNull
    @Override
    @Cacheable(cacheName = "appservice/{}/rg/{}/webapp/{}", key = "$sid/$rg/$name")
    @AzureOperation(name = "webapp.get.name|rg|sid", params = {"name", "rg"}, type = AzureOperation.Type.SERVICE)
    public IWebApp get(@NotNull String sid, @NotNull String rg, @NotNull String name) {
        return new WebApp(sid, rg, name, getAppServiceManager(sid));
    }

    @Nonnull
    @AzureOperation(name = "webapp.list_runtime.os|version", params = {"os.getValue()", "version.getValue()"}, type = AzureOperation.Type.SERVICE)
    public List<Runtime> listWebAppRuntimes(@Nonnull OperatingSystem os, @Nonnull JavaVersion version) {
        return Runtime.WEBAPP_RUNTIME.stream()
                .filter(runtime -> Objects.equals(os, runtime.getOperatingSystem()) && Objects.equals(version, runtime.getJavaVersion()))
                .collect(Collectors.toList());
    }

    // todo: share codes within app service module
    @Cacheable(cacheName = "appservice/{}/manager", key = "$subscriptionId")
    @AzureOperation(name = "appservice.get_client.subscription", params = "subscriptionId", type = AzureOperation.Type.SERVICE)
    AppServiceManager getAppServiceManager(String subscriptionId) {
        return getResourceManager(subscriptionId, AppServiceManager::configure, AppServiceManager.Configurable::authenticate);
    }

    @AzureOperation(name = "service.refresh", params = "this.name()", type = AzureOperation.Type.SERVICE)
    public void refresh() {
        try {
            CacheManager.evictCache("appservice/{}/webapps", CacheEvict.ALL);
        } catch (ExecutionException e) {
            log.warn("failed to evict cache", e);
        }
    }

    @Override
    public String name() {
        return "Azure WebApp";
    }
}
