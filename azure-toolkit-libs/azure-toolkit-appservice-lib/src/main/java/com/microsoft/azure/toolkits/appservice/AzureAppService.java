/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkits.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlot;

import java.util.List;
import java.util.stream.Collectors;

public class AzureAppService {

    private AzureResourceManager azureResourceManager;

    private AzureAppService(AzureResourceManager azureResourceManager) {
        this.azureResourceManager = azureResourceManager;
    }

    public static AzureAppService auth(AzureResourceManager azureResourceManager) {
        return new AzureAppService(azureResourceManager);
    }

    public IWebApp webapp(String id) {
        final WebAppEntity webAppEntity = WebAppEntity.builder().id(id).build();
        return webapp(webAppEntity);
    }

    public IWebApp webapp(String resourceGroup, String name) {
        final WebAppEntity webAppEntity = WebAppEntity.builder().resourceGroup(resourceGroup).name(name).build();
        return webapp(webAppEntity);
    }

    public IWebApp webapp(WebAppEntity webAppEntity) {
        return null;
    }

    public List<IWebApp> webapps() {
        return azureResourceManager.webApps().list().stream()
                .map(webAppBasic -> webapp(webAppBasic.id()))
                .collect(Collectors.toList());
    }

    public IAppServicePlan appServicePlan(AppServicePlanEntity appServicePlanEntity) {
        return null;
    }

    public IAppServicePlan appServicePlan(String id) {
        final AppServicePlanEntity appServicePlanEntity = AppServicePlanEntity.builder().id(id).build();
        return appServicePlan(appServicePlanEntity);
    }

    public IAppServicePlan appServicePlan(String resourceGroup, String name) {
        final AppServicePlanEntity appServicePlanEntity = AppServicePlanEntity.builder()
                .resourceGroup(resourceGroup)
                .name(name).build();
        return appServicePlan(appServicePlanEntity);
    }

    public List<IAppServicePlan> appServicePlans() {
        return this.azureResourceManager.appServicePlans().list().stream()
                .map(appServicePlan -> appServicePlan(appServicePlan.id()))
                .collect(Collectors.toList());
    }

    public IWebAppDeploymentSlot deploymentSlot(WebAppDeploymentSlotEntity deploymentSlot) {
        return null;
    }

    public AzureResourceManager getAzureResourceManager() {
        return azureResourceManager;
    }
}
