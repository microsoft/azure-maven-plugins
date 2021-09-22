/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.webapp.configuration;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DeploymentSlotConfig {
    private final String subscriptionId;
    private final String resourceGroup;
    private final String appName;
    private final String name;
    private final String configurationSource;
    private final Map<String, String> appSettings;
}
