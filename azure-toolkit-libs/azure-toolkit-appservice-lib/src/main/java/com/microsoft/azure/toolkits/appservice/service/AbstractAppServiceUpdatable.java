/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
public abstract class AbstractAppServiceUpdatable<T> implements AppServiceUpdatable {
    private Optional<Runtime> runtime = null;
    private Optional<PricingTier> pricingTier = null;
    private Optional<AppServicePlan> appServicePlan = null;
    private Optional<Map<String, String>> appSettings = null;

    @Override
    public AppServiceUpdatable withAppServicePlan(String appServicePlanId) {
        appServicePlan = Optional.of(AppServicePlan.builder().id(appServicePlanId).build());
        return this;
    }

    @Override
    public AppServiceUpdatable withAppServicePlan(String resourceGroup, String planName) {
        appServicePlan = Optional.of(AppServicePlan.builder().resourceGroup(resourceGroup).name(planName).build());
        return this;
    }

    @Override
    public AppServiceUpdatable<T> withRuntime(Runtime runtime) {
        this.runtime = Optional.of(runtime);
        return this;
    }

    @Override
    public AppServiceUpdatable<T> withPricingTier(PricingTier pricingTier) {
        this.pricingTier = Optional.of(pricingTier);
        return this;
    }

    @Override
    public AppServiceUpdatable<T> withAppSettings(Map appSettings) {
        this.appSettings = Optional.of(appSettings);
        return this;
    }
}
