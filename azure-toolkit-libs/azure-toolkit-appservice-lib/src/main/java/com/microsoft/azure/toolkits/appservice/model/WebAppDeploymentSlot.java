/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.microsoft.azure.toolkits.appservice.utils.ConvertUtils;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import com.microsoft.azure.tools.common.model.Subscription;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppDeploymentSlot {
    private String id;
    private String name;
    private String webappName;
    private ResourceGroup resourceGroup;
    private Subscription subscription;
    private Runtime runtime;
    private AppServicePlan appServicePlan;
    private Map<String, String> appSettings;

    public static WebAppDeploymentSlot createFromServiceModel(DeploymentSlot deploymentSlot) {
        return WebAppDeploymentSlot.builder()
                .name(deploymentSlot.name())
                .webappName(deploymentSlot.parent().name())
                .id(deploymentSlot.id())
                .resourceGroup(ResourceGroup.builder().name(deploymentSlot.resourceGroupName()).build()) // todo: Resource Group Pojo
                .subscription(Subscription.builder().id(deploymentSlot.id()).build()) // todo: Subscription Pojo
                .runtime(Runtime.createFromServiceInstance(deploymentSlot))
                .appServicePlan(AppServicePlan.builder().id(deploymentSlot.appServicePlanId()).build())
                .appSettings(ConvertUtils.normalizeAppSettings(deploymentSlot.getAppSettings()))
                .build();
    }
}
