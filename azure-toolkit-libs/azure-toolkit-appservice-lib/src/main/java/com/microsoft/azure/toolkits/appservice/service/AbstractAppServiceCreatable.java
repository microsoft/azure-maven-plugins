/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;
import com.microsoft.azure.tools.common.model.Region;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import com.microsoft.azure.tools.common.model.Subscription;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
public abstract class AbstractAppServiceCreatable<T> implements AppServiceCreatable.WithName<T>, AppServiceCreatable<T>,
        AppServiceCreatable.WithSubscription<T>, AppServiceCreatable.WithResourceGroup<T>, AppServiceCreatable.WithAppServicePlan<T>,
        AppServiceCreatable.WithRuntime<T> {

    private static final Region DEFAULT_REGION = Region.EUROPE_WEST;
    private static final PricingTier DEFAULT_PRICING = PricingTier.BASIC_B1;

    private String name = null;
    private Subscription subscription = null;
    private ResourceGroup resourceGroup = null;
    private AppServicePlan appServicePlan = null;
    private Runtime runtime = null;
    private Region region = DEFAULT_REGION;
    private PricingTier pricingTier = DEFAULT_PRICING;
    private Optional<Map<String, String>> appSettings = null;

    @Override
    public WithSubscription withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public WithResourceGroup<T> withSubscription(Subscription subscription) {
        this.subscription = subscription;
        return this;
    }

    @Override
    public WithAppServicePlan<T> withResourceGroup(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
        return this;
    }

    @Override
    public WithRuntime<T> withAppServicePlan(AppServicePlan appServicePlan) {
        this.appServicePlan = appServicePlan;
        return this;
    }

    @Override
    public AppServiceCreatable<T> withRuntime(Runtime runtime) {
        this.runtime = runtime;
        return this;
    }

    @Override
    public AppServiceCreatable withAppSettings(Map<String, String> appSettings) {
        this.appSettings = Optional.ofNullable(appSettings);
        return this;
    }

    @Override
    public AppServiceCreatable withRegion(Region region) {
        this.region = region;
        return this;
    }

    @Override
    public AppServiceCreatable withPricingTier(PricingTier pricingTier) {
        this.pricingTier = pricingTier;
        return this;
    }

    @Override
    public abstract T create();
}
