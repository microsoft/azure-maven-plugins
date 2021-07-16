/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppServicePlan implements IAppServicePlan {

    @Getter
    @Nonnull
    protected String name;
    @Getter
    @Nonnull
    protected String resourceGroup;
    @Getter
    @Nonnull
    protected String subscriptionId;

    private AppServicePlanEntity entity;
    private final AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.AppServicePlan remote;


    public AppServicePlan(@Nonnull final String id, @Nonnull final AzureResourceManager azureClient) {
        final ResourceId resourceId = ResourceId.fromString(id);
        this.name = resourceId.name();
        this.resourceGroup = resourceId.resourceGroupName();
        this.subscriptionId = resourceId.subscriptionId();
        this.azureClient = azureClient;
    }

    public AppServicePlan(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name,
                       @Nonnull final AzureResourceManager azureClient) {
        this.name = name;
        this.resourceGroup = resourceGroup;
        this.subscriptionId = subscriptionId;
        this.azureClient = azureClient;
    }

    public AppServicePlan(com.azure.resourcemanager.appservice.models.AppServicePlan remote, AzureResourceManager azureClient) {
        this.name = remote.name();
        this.resourceGroup = remote.resourceGroupName();
        this.subscriptionId = Utils.getSubscriptionId(remote.id());
        this.remote = remote;
        this.entity = AppServiceUtils.fromAppServicePlan(remote);
        this.azureClient = azureClient;
    }

    @Override
    public Creator create() {
        return new AppServicePlanCreator();
    }

    @Override
    public AppServicePlan refresh() {
        this.remote = remote();
        this.entity = Optional.ofNullable(this.remote).map(AppServiceUtils::fromAppServicePlan)
                .orElseThrow(() -> new AzureToolkitRuntimeException("Target resource does not exist."));
        return this;
    }

    @Override
    public String name() {
        return getRemoteResource().name();
    }

    @Override
    public String id() {
        return getRemoteResource().id();
    }

    @Override
    public boolean exists() {
        try {
            return remote() != null;
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            return false;
        }
    }

    @Override
    public AppServicePlanEntity entity() {
        if (remote == null) {
            refresh();
        }
        return entity;
    }

    @Override
    public List<IWebApp> webapps() {
        return getRemoteResource().manager().webApps().list().stream()
            .filter(webapp -> StringUtils.equals(webapp.appServicePlanId(), getRemoteResource().id()))
            .map(webapp -> new WebApp(webapp, azureClient))
            .collect(Collectors.toList());
    }

    @Override
    public AppServicePlanUpdater update() {
        return new AppServicePlanUpdater();
    }

    private com.azure.resourcemanager.appservice.models.AppServicePlan remote() {
        return azureClient.appServicePlans().getByResourceGroup(resourceGroup, name);
    }

    @Nonnull
    private com.azure.resourcemanager.appservice.models.AppServicePlan getRemoteResource() {
        if (remote == null) {
            refresh();
        }
        return Objects.requireNonNull(remote, "Target resource does not exist.");
    }

    public class AppServicePlanCreator implements Creator {
        private String name;
        private Region region;
        private String resourceGroup;
        private PricingTier pricingTier;
        private OperatingSystem operatingSystem;

        @Override
        public Creator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Creator withRegion(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Creator withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

        @Override
        public Creator withPricingTier(PricingTier pricingTier) {
            this.pricingTier = pricingTier;
            return this;
        }

        @Override
        public Creator withOperatingSystem(OperatingSystem operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        @Override
        public IAppServicePlan commit() {
            AppServicePlan.this.remote = azureClient.appServicePlans().define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup)
                .withPricingTier(AppServiceUtils.toPricingTier(pricingTier))
                .withOperatingSystem(convertOS(operatingSystem)).create();
            AppServicePlan.this.entity = AppServiceUtils.fromAppServicePlan(AppServicePlan.this.remote);
            return AppServicePlan.this;
        }

        private com.azure.resourcemanager.appservice.models.OperatingSystem convertOS(OperatingSystem operatingSystem) {
            return operatingSystem == OperatingSystem.WINDOWS ?
                com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS :
                com.azure.resourcemanager.appservice.models.OperatingSystem.LINUX; // Using Linux for Docker app service
        }
    }

    public class AppServicePlanUpdater implements Updater {
        private PricingTier pricingTier;

        public AppServicePlanUpdater withPricingTier(PricingTier pricingTier) {
            this.pricingTier = pricingTier;
            return this;
        }

        @Override
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
            return AppServicePlan.this;
        }
    }
}
