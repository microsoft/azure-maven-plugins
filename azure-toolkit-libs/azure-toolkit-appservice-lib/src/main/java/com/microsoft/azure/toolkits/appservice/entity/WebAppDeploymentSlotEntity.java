/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.entity;

import com.microsoft.azure.toolkits.appservice.model.Runtime;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppDeploymentSlotEntity {
    private String id;
    private String name;
    private String webappName;
    private String resourceGroup;
    private String subscriptionId;
    private String appServicePlanId;
    private Runtime runtime;
    private String defaultHostName;
    private Map<String, String> appSettings;
}
