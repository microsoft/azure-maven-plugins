/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.microsoft.azure.toolkits.appservice.utils.Utils;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppDeploymentSlot {
    private String id;
    private String name;
    private String webappName;
    private String resourceGroup;
    private String subscriptionId;
    private String appServicePlanId;
    private Runtime runtime;
    private Map<String, String> appSettings;

    public static WebAppDeploymentSlot createFromServiceModel(DeploymentSlot deploymentSlot) {
        return WebAppDeploymentSlot.builder()
                .name(deploymentSlot.name())
                .webappName(deploymentSlot.parent().name())
                .id(deploymentSlot.id())
                .resourceGroup(deploymentSlot.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(deploymentSlot.id()))
                .runtime(Runtime.createFromServiceInstance(deploymentSlot))
                .appServicePlanId(deploymentSlot.appServicePlanId())
                .appSettings(Utils.normalizeAppSettings(deploymentSlot.getAppSettings()))
                .build();
    }
}
