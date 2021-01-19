/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder(toBuilder = true)
public class AppServicePlan {
    private String id;
    private String name;
    private String region;
    private String resourceGroup;
    private PricingTier pricingTier;
    private OperatingSystem operatingSystem;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AppServicePlan)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final AppServicePlan target = (AppServicePlan) obj;
        return StringUtils.equals(target.getId(), this.getId()) ||
                StringUtils.equals(target.getResourceGroup(), this.getResourceGroup()) && StringUtils.equals(target.getName(), this.getName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static AppServicePlan createFromServiceModel(com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan) {
        return AppServicePlan.builder()
                .id(appServicePlan.id())
                .name(appServicePlan.name())
                .region(appServicePlan.regionName())
                .resourceGroup(appServicePlan.resourceGroupName())
                .pricingTier(PricingTier.createFromServiceModel(appServicePlan.pricingTier()))
                .operatingSystem(OperatingSystem.getFromServiceModel(appServicePlan.operatingSystem()))
                .build();
    }
}
