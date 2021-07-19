/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public class AppServiceConfig {

    private String subscriptionId;

    private String resourceGroup;

    private Region region;

    private PricingTier pricingTier;

    private String appName;

    private String servicePlanResourceGroup;

    private String servicePlanName;

    private RuntimeConfig runtime;

    private Map<String, String> appSettings;

    private String deploymentSlotName;

    private String deploymentSlotConfigurationSource;

    public AppServicePlanConfig getServicePlanConfig() {
        return new AppServicePlanConfig()
            .subscriptionId(subscriptionId())
            .servicePlanResourceGroup(servicePlanResourceGroup())
            .servicePlanName(servicePlanName())
            .region(region())
            .os(runtime() == null ? null : runtime().os())
            .pricingTier(pricingTier());
    }
}
