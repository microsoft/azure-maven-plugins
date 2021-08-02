/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import java.util.List;

public interface IAppServicePlan extends IAzureResource<AppServicePlanEntity> {
    Creator create();

    Updater update();

    boolean exists();

    AppServicePlanEntity entity();

    List<IWebApp> webapps();

    interface Creator {
        Creator withName(String name);

        Creator withRegion(Region region);

        Creator withResourceGroup(String resourceGroupName);

        Creator withPricingTier(PricingTier pricingTier);

        Creator withOperatingSystem(OperatingSystem operatingSystem);

        IAppServicePlan commit();
    }

    interface Updater {
        Updater withPricingTier(PricingTier pricingTier);

        IAppServicePlan commit();
    }
}
