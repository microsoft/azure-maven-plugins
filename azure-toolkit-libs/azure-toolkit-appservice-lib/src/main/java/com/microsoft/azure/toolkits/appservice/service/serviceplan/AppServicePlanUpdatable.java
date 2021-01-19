/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.serviceplan;

import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;

import java.util.Optional;

public class AppServicePlanUpdatable {
    private Optional<PricingTier> pricingTier;
    private com.azure.resourcemanager.appservice.models.AppServicePlan planService;

    public AppServicePlanUpdatable(com.azure.resourcemanager.appservice.models.AppServicePlan planService) {
        this.planService = planService;
    }

    public AppServicePlanUpdatable withPricingTier(PricingTier pricingTier) {
        this.pricingTier = Optional.of(pricingTier);
        return this;
    }

    public AppServicePlan update() {
        com.azure.resourcemanager.appservice.models.AppServicePlan service = planService;
        if (pricingTier != null && pricingTier.isPresent()) {
            final com.azure.resourcemanager.appservice.models.PricingTier newPricingTier = PricingTier.convertToServiceModel(pricingTier.get());
            if (newPricingTier != planService.pricingTier()) {
                service = planService.update().withPricingTier(newPricingTier).apply();
            }
        }
        return AppServicePlan.createFromServiceModel(service);
    }
}
