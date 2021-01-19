/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.service.AppServicePlanManager;
import com.microsoft.azure.toolkits.appservice.service.AppServicePlansManager;
import com.microsoft.azure.toolkits.appservice.service.WebAppDeploymentSlotManager;
import com.microsoft.azure.toolkits.appservice.service.WebAppManager;
import com.microsoft.azure.toolkits.appservice.service.WebAppsManager;
import com.microsoft.azure.toolkits.appservice.service.deploymentslot.WebAppDeploymentSlotManagerImpl;
import com.microsoft.azure.toolkits.appservice.service.serviceplan.AppServicePlanManagerImpl;
import com.microsoft.azure.toolkits.appservice.service.serviceplan.AppServicePlansManagerImpl;
import com.microsoft.azure.toolkits.appservice.service.webapp.WebAppManagerImpl;
import com.microsoft.azure.toolkits.appservice.service.webapp.WebAppsManagerImpl;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;
import com.microsoft.azure.toolkits.appservice.model.WebApp;
import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;
import com.microsoft.azure.tools.common.model.ResourceGroup;

public class AppService {

    private AzureResourceManager azureResourceManager;

    private AppService(AzureResourceManager azureResourceManager) {
        this.azureResourceManager = azureResourceManager;
    }

    public static AppService auth(AzureResourceManager azureResourceManager) {
        return new AppService(azureResourceManager);
    }

    public WebAppsManager webapps() {
        return WebAppsManagerImpl.getInstance(this);
    }

    public WebAppManager webapp(String id) {
        final WebApp webApp = WebApp.builder().id(id).build();
        return webapp(webApp);
    }

    public WebAppManager webapp(String resourceGroup, String name) {
        final WebApp webApp = WebApp.builder()
                .resourceGroup(ResourceGroup.builder().name(resourceGroup).build())
                .name(name).build();
        return webapp(webApp);
    }

    public WebAppManager webapp(WebApp webApp) {
        return new WebAppManagerImpl(webApp, this);
    }

    public AppServicePlansManager appServicePlans() {
        return AppServicePlansManagerImpl.getInstance(this);
    }

    public AppServicePlanManager appServicePlan(AppServicePlan appServicePlan) {
        return new AppServicePlanManagerImpl(appServicePlan, this);
    }

    public AppServicePlanManager appServicePlan(String id) {
        final AppServicePlan appServicePlan = AppServicePlan.builder().id(id).build();
        return appServicePlan(appServicePlan);
    }

    public AppServicePlanManager appServicePlan(String resourceGroup, String name) {
        final AppServicePlan appServicePlan = AppServicePlan.builder()
                .resourceGroup(ResourceGroup.builder().name(resourceGroup).build())
                .name(name).build();
        return appServicePlan(appServicePlan);
    }

    public WebAppDeploymentSlotManager deploymentSlot(WebAppDeploymentSlot deploymentSlot) {
        return new WebAppDeploymentSlotManagerImpl(deploymentSlot, this);
    }

    public AzureResourceManager getAzureResourceManager() {
        return azureResourceManager;
    }
}
