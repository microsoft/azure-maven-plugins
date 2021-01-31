/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.AzureAppService;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlanCreator;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlanUpdater;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;

import java.util.List;

public class AppServicePlan implements IAppServicePlan {

    private AppServicePlanEntity entity;
    private AzureAppService azureAppService;
    private AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlanClient;

    public AppServicePlan(AppServicePlanEntity appServicePlanEntity, AzureAppService appService) {
        this.entity = appServicePlanEntity;
        this.azureAppService = appService;
        this.azureClient = appService.getAzureResourceManager();
    }

    @Override
    public IAppServicePlanCreator create() {
        return null;
    }

    @Override
    public IAppServicePlanUpdater update() {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public AppServicePlanEntity entity() {
        return null;
    }

    @Override
    public List<IWebApp> webapps() {
        return null;
    }

    @Override
    public String id() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }
}
