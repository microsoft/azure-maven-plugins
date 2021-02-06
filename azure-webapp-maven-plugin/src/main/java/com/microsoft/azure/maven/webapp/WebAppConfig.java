/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppConfig {
    private String subscriptionId;
    private String resourceGroup;
    private String appName;
    private String servicePlanName;
    private String servicePlanResourceGroup;
    private Region region;
    private PricingTier pricingTier;
    private Runtime runtime;
    private DockerConfiguration dockerConfiguration;
    private String deploymentSlotName;
    private String deploymentSlotConfigurationSource;
    private Map<String, String> appSettings;
    // resources
    private List<WebAppArtifact> webAppArtifacts;
}
