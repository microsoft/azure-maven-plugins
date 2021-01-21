/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp;

import com.azure.resourcemanager.appservice.models.DeployType;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.tools.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppConfig {
    private String schemaVersion;

    private String subscriptionId;
    private String resourceGroup;
    private String appName;
    private String servicePlanName;
    private String servicePlanResourceGroup;
    private Region region;
    private PricingTier pricingTier;
    private Runtime runtime;
    private DeployType deployType;
    private DockerConfiguration dockerConfiguration;
    private String deploymentSlotName;
    private String deploymentSlotConfigurationSource;

    // web app runtime related configurations
    private List<Resource> resources;
    private String stagingDirectoryPath;
    private String buildDirectoryAbsolutePath;
    private MavenProject project;
    private MavenSession session;
    private MavenResourcesFiltering filtering;
}
