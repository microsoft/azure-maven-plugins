/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.entity;

import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class AppServiceDeploymentSlotBaseEntity implements IAzureResourceEntity {
    private final String id;
    private final String name;
    private final String subscriptionId;
    private final String resourceGroup;
    private final String appServicePlanId;
    private final Runtime runtime;
    private final String defaultHostName;
    private final Map<String, String> appSettings;
}
