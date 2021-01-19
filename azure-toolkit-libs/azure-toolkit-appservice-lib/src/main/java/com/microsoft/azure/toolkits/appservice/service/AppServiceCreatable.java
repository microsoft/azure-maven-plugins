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

import java.util.Map;

public interface AppServiceCreatable<T> {
    interface WithSubscription<T> {
        WithResourceGroup<T> withSubscription(Subscription subscription);
    }

    interface WithResourceGroup<T> {
        WithAppServicePlan<T> withResourceGroup(ResourceGroup resourceGroup);
    }

    interface WithAppServicePlan<T> {
        WithRuntime<T> withAppServicePlan(AppServicePlan appServicePlan);
    }

    interface WithRuntime<T> {
        AppServiceCreatable<T> withRuntime(Runtime runtime);
    }

    interface WithName<T> {
        WithSubscription withName(String name);
    }

    AppServiceCreatable<T> withRegion(Region region);

    AppServiceCreatable<T> withPricingTier(PricingTier region);

    AppServiceCreatable<T> withAppSettings(Map<String, String> appSettings);

    T create();
}
