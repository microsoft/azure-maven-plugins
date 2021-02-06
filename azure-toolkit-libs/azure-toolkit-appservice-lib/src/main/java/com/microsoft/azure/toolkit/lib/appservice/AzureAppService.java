/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;

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
        return new WebApp(webAppEntity, this);
    }

    public List<IWebApp> webapps() {
        return azureResourceManager.webApps().list().stream()
            .map(webAppBasic -> webapp(webAppBasic.id()))
            .collect(Collectors.toList());
    }

    public IAppServicePlan appServicePlan(AppServicePlanEntity appServicePlanEntity) {
        return new AppServicePlan(appServicePlanEntity, this);
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

    public IWebAppDeploymentSlot deploymentSlot(String id) {
        return deploymentSlot(WebAppDeploymentSlotEntity.builder().id(id).build());
    }

    public IWebAppDeploymentSlot deploymentSlot(String resourceGroup, String appName, String slotName) {
        return deploymentSlot(WebAppDeploymentSlotEntity.builder().resourceGroup(resourceGroup).webappName(appName).name(slotName).build());
    }

    public IWebAppDeploymentSlot deploymentSlot(WebAppDeploymentSlotEntity deploymentSlot) {
        return new WebAppDeploymentSlot(deploymentSlot, this);
    }

    public AzureResourceManager getAzureResourceManager() {
        return azureResourceManager;
    }
}
