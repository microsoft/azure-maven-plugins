/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.entity;

import com.microsoft.azure.toolkits.appservice.model.Runtime;
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
    private String defaultHostName;
    private Map<String, String> appSettings;
}
