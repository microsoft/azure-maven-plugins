/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.model;

import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationInsightsConfig {
    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String instrumentationKey;
    private Boolean createNewInstance;
    private Boolean disableAppInsights;
    private LogAnalyticsWorkspaceConfig workspaceConfig;
}
