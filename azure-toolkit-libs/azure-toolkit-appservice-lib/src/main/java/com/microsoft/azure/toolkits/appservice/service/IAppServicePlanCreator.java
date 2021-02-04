/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;


import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.tools.common.model.Region;

public interface IAppServicePlanCreator {
    IAppServicePlanCreator withName(String name);

    IAppServicePlanCreator withRegion(Region region);

    IAppServicePlanCreator withResourceGroup(String resourceGroupName);

    IAppServicePlanCreator withPricingTier(PricingTier pricingTier);

    IAppServicePlanCreator withOperatingSystem(OperatingSystem operatingSystem);

    IAppServicePlan commit();
}
