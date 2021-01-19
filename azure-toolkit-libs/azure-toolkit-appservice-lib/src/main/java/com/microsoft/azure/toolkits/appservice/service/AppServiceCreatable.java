/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.tools.common.model.Region;

import java.util.Map;

public interface AppServiceCreatable<T> {
    interface WithSubscription<T> {
        WithResourceGroup<T> withSubscription(String subscriptionId);
    }

    interface WithResourceGroup<T> {
        WithAppServicePlan<T> withResourceGroup(String resourceGroupName);
    }

    interface WithAppServicePlan<T> {
        WithRuntime<T> withAppServicePlan(String appServicePlanId);

        WithRuntime<T> withAppServicePlan(String resourceGroup, String planName);
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
