/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.entity;

import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class AppServicePlanEntity {
    private String id;
    private String name;
    private String region;
    private String resourceGroup;
    private PricingTier pricingTier;
    private OperatingSystem operatingSystem;

    public static AppServicePlanEntity createFromServiceModel(com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan) {
        return AppServicePlanEntity.builder()
                .id(appServicePlan.id())
                .name(appServicePlan.name())
                .region(appServicePlan.regionName())
                .resourceGroup(appServicePlan.resourceGroupName())
                .pricingTier(PricingTier.createFromServiceModel(appServicePlan.pricingTier()))
                .operatingSystem(OperatingSystem.getFromServiceModel(appServicePlan.operatingSystem()))
                .build();
    }
}
