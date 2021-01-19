/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;

import java.util.Map;

public interface AppServiceUpdatable<T> {
    AppServiceUpdatable<T> withAppServicePlan(AppServicePlan subscription);

    AppServiceUpdatable<T> withRuntime(Runtime runtime);

    AppServiceUpdatable<T> withPricingTier(PricingTier region);

    AppServiceUpdatable<T> withAppSettings(Map<String, String> appSettings);

    T update();
}
