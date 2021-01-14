/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.appservice.model;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import com.microsoft.azure.tools.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppDeploymentSlot {
    @Setter
    private WebApp webApp;

    private String name;
    private String id;
    private ResourceGroup resourceGroup;
    private Subscription subscription;
    private Runtime runtime;
    private AppServicePlan appServicePlan;
    private Map<String, String> appSettings;

    public WebAppDeploymentSlot withWebApp(WebApp webApp) {
        this.webApp = webApp;
        return this;
    }

    public static WebAppDeploymentSlot createFromServiceModel(DeploymentSlot deploymentSlot) {
        return WebAppDeploymentSlot.builder()
                .name(deploymentSlot.name())
                .id(deploymentSlot.id())
                .resourceGroup(ResourceGroup.builder().name(deploymentSlot.resourceGroupName()).build()) // todo: Resource Group Pojo
                .subscription(Subscription.builder().id(deploymentSlot.id()).build()) // todo: Subscription Pojo
                .runtime(Runtime.createFromServiceInstance(deploymentSlot))
                .appServicePlan(AppServicePlan.builder().id(deploymentSlot.appServicePlanId()).build())
                .build();
    }
}
