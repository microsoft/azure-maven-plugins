/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlanCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlanUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppServicePlan implements IAppServicePlan {

    private AppServicePlanEntity entity;
    private AzureAppService azureAppService;
    private AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlanInner;

    public AppServicePlan(AppServicePlanEntity appServicePlanEntity, AzureAppService appService) {
        this.entity = appServicePlanEntity;
        this.azureAppService = appService;
        this.azureClient = appService.getAzureResourceManager();
    }

    @Override
    public IAppServicePlanCreator create() {
        return new AppServicePlanCreator();
    }

    @Override
    public boolean exists() {
        refreshAppServicePlanInner();
        return appServicePlanInner != null;
    }

    @Override
    public AppServicePlanEntity entity() {
        return entity;
    }

    @Override
    public List<IWebApp> webapps() {
        return getAppServicePlanInner().manager().webApps().list().stream()
            .filter(webapp -> StringUtils.equals(webapp.appServicePlanId(), getAppServicePlanInner().id()))
            .map(webapp -> new WebApp(AppServiceUtils.fromWebAppBasic(webapp), azureAppService))
            .collect(Collectors.toList());
    }

    @Override
    public AppServicePlanUpdater update() {
        return new AppServicePlanUpdater();
    }

    public com.azure.resourcemanager.appservice.models.AppServicePlan getAppServicePlanInner() {
        if (appServicePlanInner == null) {
            refreshAppServicePlanInner();
        }
        return appServicePlanInner;
    }

    public synchronized void refreshAppServicePlanInner() {
        appServicePlanInner = AppServiceUtils.getAppServicePlan(entity, azureClient);
    }

    @Override
    public String id() {
        return getAppServicePlanInner().id();
    }

    @Override
    public String name() {
        return getAppServicePlanInner().name();
    }

    public class AppServicePlanCreator implements IAppServicePlanCreator {
        private String name;
        private Region region;
        private String resourceGroup;
        private PricingTier pricingTier;
        private OperatingSystem operatingSystem;

        @Override
        public IAppServicePlanCreator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public IAppServicePlanCreator withRegion(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public IAppServicePlanCreator withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

        @Override
        public IAppServicePlanCreator withPricingTier(PricingTier pricingTier) {
            this.pricingTier = pricingTier;
            return this;
        }

        @Override
        public IAppServicePlanCreator withOperatingSystem(OperatingSystem operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        @Override
        public IAppServicePlan commit() {
            AppServicePlan.this.appServicePlanInner = azureClient.appServicePlans().define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup)
                .withPricingTier(AppServiceUtils.toPricingTier(pricingTier))
                .withOperatingSystem(convertOS(operatingSystem)).create();
            AppServicePlan.this.entity = AppServiceUtils.fromAppServicePlan(AppServicePlan.this.appServicePlanInner);
            return AppServicePlan.this;
        }

        private com.azure.resourcemanager.appservice.models.OperatingSystem convertOS(OperatingSystem operatingSystem) {
            return operatingSystem == OperatingSystem.WINDOWS ?
                com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS :
                com.azure.resourcemanager.appservice.models.OperatingSystem.LINUX; // Using Linux for Docker app service
        }
    }

    public class AppServicePlanUpdater implements IAppServicePlanUpdater {
        private Optional<PricingTier> pricingTier;

        public AppServicePlanUpdater withPricingTier(PricingTier pricingTier) {
            this.pricingTier = Optional.of(pricingTier);
            return this;
        }

        @Override
        public AppServicePlan commit() {
            boolean modified = false;
            com.azure.resourcemanager.appservice.models.AppServicePlan.Update update = appServicePlanInner.update();
            if (pricingTier != null && pricingTier.isPresent()) {
                final com.azure.resourcemanager.appservice.models.PricingTier newPricingTier = AppServiceUtils.toPricingTier(pricingTier.get());
                if (!Objects.equals(newPricingTier, AppServicePlan.this.getAppServicePlanInner().pricingTier())) {
                    modified = true;
                    update = update.withPricingTier(newPricingTier);
                }
            }
            if (modified) {
                AppServicePlan.this.appServicePlanInner = update.apply();
            }
            AppServicePlan.this.entity = AppServiceUtils.fromAppServicePlan(AppServicePlan.this.appServicePlanInner);
            return AppServicePlan.this;
        }
    }
}
