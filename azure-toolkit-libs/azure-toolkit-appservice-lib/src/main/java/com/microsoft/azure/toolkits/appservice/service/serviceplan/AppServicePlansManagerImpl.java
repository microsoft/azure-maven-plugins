/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.serviceplan;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.service.AppServicePlansManager;
import com.microsoft.azure.toolkits.appservice.AppService;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppServicePlansManagerImpl implements AppServicePlansManager {
    private static final Map<AppService, AppServicePlansManagerImpl> map = new HashMap<>();

    private AppService appService;
    private AzureResourceManager azureResourceManager;

    private AppServicePlansManagerImpl(AppService appService) {
        this.appService = appService;
        this.azureResourceManager = appService.getAzureResourceManager();
    }

    public static AppServicePlansManagerImpl getInstance(AppService appService) {
        return map.computeIfAbsent(appService, key -> new AppServicePlansManagerImpl(key));
    }

    public AppServicePlan get(String resourceGroup, String name) {
        return AppServicePlan.createFromServiceModel(azureResourceManager.appServicePlans().getByResourceGroup(resourceGroup, name));
    }

    public AppServicePlan get(String id) {
        return AppServicePlan.createFromServiceModel(azureResourceManager.appServicePlans().getById(id));
    }

    public List<AppServicePlan> list() {
        return azureResourceManager.appServicePlans().list().stream().map(AppServicePlan::createFromServiceModel).collect(Collectors.toList());
    }
}
