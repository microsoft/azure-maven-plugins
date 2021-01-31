/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.AzureAppService;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlanCreator;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlanUpdater;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;
import com.microsoft.azure.tools.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppServicePlan implements IAppServicePlan {

    private AppServicePlanEntity entity;
    private AzureAppService azureAppService;
    private AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlanClient;

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
        return getPlanService(true) != null;
    }

    @Override
    public AppServicePlanEntity entity() {
        return entity;
    }

    @Override
    public List<IWebApp> webapps() {
        return getPlanService().manager().webApps().list().stream()
                .filter(webapp -> StringUtils.equals(webapp.appServicePlanId(), getPlanService().id()))
                .map(webapp -> new WebApp(AppServiceUtils.getBasicWebAppEntity(webapp), azureAppService))
                .collect(Collectors.toList());
    }

    @Override
    public AppServicePlanUpdater update() {
        return new AppServicePlanUpdater();
    }

    public com.azure.resourcemanager.appservice.models.AppServicePlan getPlanService() {
        return getPlanService(false);
    }

    public synchronized com.azure.resourcemanager.appservice.models.AppServicePlan getPlanService(boolean force) {
        if (appServicePlanClient == null || force) {
            appServicePlanClient = AppServiceUtils.getAppServicePlan(entity, azureClient);
        }
        return appServicePlanClient;
    }

    @Override
    public String id() {
        return getPlanService().id();
    }

    @Override
    public String name() {
        return getPlanService().name();
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
            AppServicePlan.this.appServicePlanClient = azureClient.appServicePlans().define(name)
                    .withRegion(region.getName())
                    .withExistingResourceGroup(resourceGroup)
                    .withPricingTier(AppServiceUtils.toPricingTier(pricingTier))
                    .withOperatingSystem(convertOS(operatingSystem)).create();
            AppServicePlan.this.entity = AppServiceUtils.getAppServicePlanEntity(AppServicePlan.this.appServicePlanClient);
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
            com.azure.resourcemanager.appservice.models.AppServicePlan.Update update = appServicePlanClient.update();
            if (pricingTier != null && pricingTier.isPresent()) {
                final com.azure.resourcemanager.appservice.models.PricingTier newPricingTier = AppServiceUtils.toPricingTier(pricingTier.get());
                if (newPricingTier != AppServicePlan.this.getPlanService().pricingTier()) {
                    modified = true;
                    update = update.withPricingTier(newPricingTier);
                }
            }
            if (modified) {
                AppServicePlan.this.appServicePlanClient = update.apply();
            }
            AppServicePlan.this.entity = AppServiceUtils.getAppServicePlanEntity(AppServicePlan.this.appServicePlanClient);
            return AppServicePlan.this;
        }
    }
}
