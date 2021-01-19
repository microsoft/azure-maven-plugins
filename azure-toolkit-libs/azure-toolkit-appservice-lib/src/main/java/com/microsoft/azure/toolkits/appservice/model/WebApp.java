/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.microsoft.azure.toolkits.appservice.utils.ConvertUtils;
import com.microsoft.azure.tools.common.model.Region;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import com.microsoft.azure.tools.common.model.Subscription;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebApp {
    private String name;
    private String id;
    private Region region;
    private ResourceGroup resourceGroup;
    private Subscription subscription;
    private Runtime runtime;
    private AppServicePlan appServicePlan;
    private Map<String, String> appSettings;

    public static WebApp createFromWebAppBase(WebAppBase webAppBase) {
        return builder().name(webAppBase.name())
                .id(webAppBase.id())
                .region(Region.fromName(webAppBase.regionName()))
                .resourceGroup(ResourceGroup.builder().name(webAppBase.resourceGroupName()).build())
                .subscription(Subscription.builder().id(webAppBase.id()).build())
                .runtime(Runtime.createFromServiceInstance(webAppBase))
                .appServicePlan(AppServicePlan.builder().id(webAppBase.appServicePlanId()).build())
                .appSettings(ConvertUtils.normalizeAppSettings(webAppBase.getAppSettings()))
                .build();
    }

    public static WebApp createFromWebAppBasic(WebAppBasic webAppBasic) {
        return builder().name(webAppBasic.name())
                .id(webAppBasic.id())
                .region(Region.fromName(webAppBasic.regionName()))
                .resourceGroup(ResourceGroup.builder().name(webAppBasic.resourceGroupName()).build())
                .subscription(Subscription.builder().id(webAppBasic.id()).build())
                .appServicePlan(AppServicePlan.builder().id(webAppBasic.appServicePlanId()).build())
                .build();
    }
}
