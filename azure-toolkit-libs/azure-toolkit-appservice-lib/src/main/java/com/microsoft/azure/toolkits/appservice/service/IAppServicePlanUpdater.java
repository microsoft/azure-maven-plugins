/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.service.serviceplan.AppServicePlan;

public interface IAppServicePlanUpdater {
    IAppServicePlanUpdater withPricingTier(PricingTier pricingTier);

    AppServicePlan apply();
}
