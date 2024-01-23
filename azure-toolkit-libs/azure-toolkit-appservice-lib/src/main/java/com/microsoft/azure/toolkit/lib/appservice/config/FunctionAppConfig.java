/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.ContainerAppFunctionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class FunctionAppConfig extends AppServiceConfig {
    private String appInsightsInstance;
    private String appInsightsKey;
    private boolean disableAppInsights;
    private Boolean enableDistributedTracing;
    private String storageAccountName;
    private String storageAccountResourceGroup;
    private String environment;
    private ContainerAppFunctionConfiguration containerConfiguration;
    private LogAnalyticsWorkspaceConfig workspaceConfig;
    private FlexConsumptionConfiguration flexConsumptionConfiguration;
}
