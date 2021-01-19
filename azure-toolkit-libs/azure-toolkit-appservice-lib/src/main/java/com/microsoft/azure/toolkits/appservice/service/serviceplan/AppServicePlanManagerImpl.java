/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.serviceplan;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.AppService;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.AppServicePlanManager;
import org.apache.commons.lang3.StringUtils;

public class AppServicePlanManagerImpl implements AppServicePlanManager {

    private AppServicePlan appServicePlan;
    private AppService appService;
    private AzureResourceManager azureResourceManager;
    private com.azure.resourcemanager.appservice.models.AppServicePlan planService;

    public AppServicePlanManagerImpl(AppServicePlan appServicePlan, AppService appService) {
        this.appServicePlan = appServicePlan;
        this.appService = appService;
        this.azureResourceManager = appService.getAzureResourceManager();
    }

    @Override
    public boolean exists() {
        return getPlanService(true) != null;
    }

    @Override
    public AppServicePlan get() {
        this.appServicePlan = AppServicePlan.createFromServiceModel(getPlanService());
        return appServicePlan;
    }

    @Override
    public AppServicePlanUpdatable update() {
        return new AppServicePlanUpdatable(getPlanService());
    }

    public com.azure.resourcemanager.appservice.models.AppServicePlan getPlanService() {
        return getPlanService(false);
    }

    public synchronized com.azure.resourcemanager.appservice.models.AppServicePlan getPlanService(boolean force) {
        if (planService == null || force) {
            planService = StringUtils.isEmpty(appServicePlan.getId()) ?
                    azureResourceManager.appServicePlans().getById(appServicePlan.getId()) :
                    azureResourceManager.appServicePlans().getByResourceGroup(appServicePlan.getResourceGroup(), appServicePlan.getName());
        }
        return planService;
    }
}
