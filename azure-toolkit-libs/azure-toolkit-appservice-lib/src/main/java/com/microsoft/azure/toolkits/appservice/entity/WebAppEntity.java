/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.entity;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.utils.Utils;
import com.microsoft.azure.tools.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppEntity {
    private String name;
    private String id;
    private Region region;
    private String resourceGroup;
    private String subscriptionId;
    private Runtime runtime;
    private String appServicePlanId;
    private Map<String, String> appSettings;

    public static WebAppEntity createFromWebAppBase(WebAppBase webAppBase) {
        return builder().name(webAppBase.name())
                .id(webAppBase.id())
                .region(Region.fromName(webAppBase.regionName()))
                .resourceGroup(webAppBase.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(webAppBase.id()))
                .runtime(null)
                .appServicePlanId(webAppBase.appServicePlanId())
                .appSettings(Utils.normalizeAppSettings(webAppBase.getAppSettings()))
                .build();
    }

    public static WebAppEntity createFromWebAppBasic(WebAppBasic webAppBasic) {
        return builder().name(webAppBasic.name())
                .id(webAppBasic.id())
                .region(Region.fromName(webAppBasic.regionName()))
                .resourceGroup(webAppBasic.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(webAppBasic.id()))
                .appServicePlanId(webAppBasic.appServicePlanId())
                .build();
    }
}
