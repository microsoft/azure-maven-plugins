/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppServicePlan extends AbstractAzureManager<com.azure.resourcemanager.appservice.models.AppServicePlan>
        implements IAzureResource<AppServicePlanEntity> {
    private static final String APP_SERVICE_PLAN_ID_TEMPLATE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/serverfarms/%s";

    private AppServicePlanEntity entity;
    private final AppServiceManager azureClient;


    public AppServicePlan(@Nonnull final String id, @Nonnull final AppServiceManager azureClient) {
        super(id);
        this.azureClient = azureClient;
    }

    public AppServicePlan(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name,
                       @Nonnull final AppServiceManager azureClient) {
        super(subscriptionId, resourceGroup, name);
        this.azureClient = azureClient;
    }

    public AppServicePlan(com.azure.resourcemanager.appservice.models.AppServicePlan remote, AppServiceManager azureClient) {
        super(remote);
        this.entity = AppServiceUtils.fromAppServicePlan(remote);
        this.azureClient = azureClient;
    }

    public AppServicePlanCreator create() {
        return new AppServicePlanCreator();
    }

    @Override
    public void refresh() {
        super.refresh();
        this.entity = Optional.ofNullable(this.remote).map(AppServiceUtils::fromAppServicePlan).orElse(null);
    }

    public String name() {
        return getRemoteResource().name();
    }

    public String id() {
        return String.format(APP_SERVICE_PLAN_ID_TEMPLATE, subscriptionId, resourceGroup, name);
    }

    public AppServicePlanEntity entity() {
        if (remote == null) {
            refresh();
        }
        return entity;
    }

    public List<WebApp> webapps() {
        return getRemoteResource().manager().webApps().list().stream()
            .filter(webapp -> StringUtils.equals(webapp.appServicePlanId(), getRemoteResource().id()))
            .map(webapp -> new WebApp(webapp, azureClient))
            .collect(Collectors.toList());
    }

    public AppServicePlanUpdater update() {
        return new AppServicePlanUpdater();
    }

    protected com.azure.resourcemanager.appservice.models.AppServicePlan loadRemote() {
        return azureClient.appServicePlans().getByResourceGroup(resourceGroup, name);
    }

    @Nonnull
    private com.azure.resourcemanager.appservice.models.AppServicePlan getRemoteResource() {
        if (remote == null) {
            refresh();
        }
        return Objects.requireNonNull(remote, "Target resource does not exist.");
    }

    public class AppServicePlanCreator {
        private String name;
        private Region region;
        private String resourceGroup;
        private PricingTier pricingTier;
        private OperatingSystem operatingSystem;

        public AppServicePlanCreator withName(String name) {
            this.name = name;
            return this;
        }

        public AppServicePlanCreator withRegion(Region region) {
            this.region = region;
            return this;
        }

        public AppServicePlanCreator withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

        public AppServicePlanCreator withPricingTier(PricingTier pricingTier) {
            this.pricingTier = pricingTier;
            return this;
        }

        public AppServicePlanCreator withOperatingSystem(OperatingSystem operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        public AppServicePlan commit() {
            AppServicePlan.this.remote = azureClient.appServicePlans().define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup)
                .withPricingTier(AppServiceUtils.toPricingTier(pricingTier))
                .withOperatingSystem(convertOS(operatingSystem)).create();
            AppServicePlan.this.entity = AppServiceUtils.fromAppServicePlan(AppServicePlan.this.remote);
            Azure.az(AzureAppServicePlan.class).refresh(); // todo: refactor to support refresh single subscription
            AppServicePlan.this.refreshStatus();
            return AppServicePlan.this;
        }

        private com.azure.resourcemanager.appservice.models.OperatingSystem convertOS(OperatingSystem operatingSystem) {
            return operatingSystem == OperatingSystem.WINDOWS ?
                com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS :
                com.azure.resourcemanager.appservice.models.OperatingSystem.LINUX; // Using Linux for Docker app service
        }
    }

    public class AppServicePlanUpdater {
        private PricingTier pricingTier;

        public AppServicePlanUpdater withPricingTier(PricingTier pricingTier) {
            this.pricingTier = pricingTier;
            return this;
        }

        public AppServicePlan commit() {
            boolean modified = false;
            com.azure.resourcemanager.appservice.models.AppServicePlan.Update update = getRemoteResource().update();
            if (pricingTier != null) {
                final com.azure.resourcemanager.appservice.models.PricingTier newPricingTier = AppServiceUtils.toPricingTier(pricingTier);
                if (!Objects.equals(newPricingTier, AppServicePlan.this.getRemoteResource().pricingTier())) {
                    modified = true;
                    update = update.withPricingTier(newPricingTier);
                }
            }
            if (modified) {
                AppServicePlan.this.remote = update.apply();
            }
            AppServicePlan.this.entity = AppServiceUtils.fromAppServicePlan(AppServicePlan.this.remote);
            AppServicePlan.this.refreshStatus();
            return AppServicePlan.this;
        }
    }
}
