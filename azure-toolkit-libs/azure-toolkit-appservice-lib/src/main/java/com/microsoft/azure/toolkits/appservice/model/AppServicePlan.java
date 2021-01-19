/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.model;

import com.microsoft.azure.tools.common.model.ResourceGroup;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder(toBuilder = true)
public class AppServicePlan {
    private String id;
    private String name;
    private String region;
    private ResourceGroup resourceGroup;
    private PricingTier pricingTier;
    private OperatingSystem operatingSystem;

    public static boolean equals(AppServicePlan first, AppServicePlan second) {
        return StringUtils.equals(first.getId(), second.getId()) ||
                StringUtils.equals(first.getResourceGroup().getName(), second.getResourceGroup().getName()) &&
                        StringUtils.equals(first.getName(), second.getName());
    }

    public static AppServicePlan createFromServiceModel(com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan) {
        return AppServicePlan.builder()
                .id(appServicePlan.id())
                .name(appServicePlan.name())
                .region(appServicePlan.regionName())
                .resourceGroup(ResourceGroup.builder().name(appServicePlan.resourceGroupName()).build())
                .pricingTier(PricingTier.createFromServiceModel(appServicePlan.pricingTier()))
                .operatingSystem(OperatingSystem.getFromServiceModel(appServicePlan.operatingSystem()))
                .build();
    }
}
