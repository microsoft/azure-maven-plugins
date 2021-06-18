/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.utils.JavaVersionUtils;
import com.microsoft.azure.maven.webapp.utils.WebContainerUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
public class WebAppConfiguration {
    public static final PricingTier DEFAULT_JBOSS_PRICING_TIER = PricingTier.PREMIUM_P1V3;
    public static final Region DEFAULT_REGION = Region.EUROPE_WEST;
    public static final PricingTier DEFAULT_PRICINGTIER = PricingTier.PREMIUM_P1V2;
    public static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8;
    public static final WebContainer DEFAULT_CONTAINER = WebContainer.TOMCAT_85;

    // artifact deploy related configurations
    protected String subscriptionId;
    protected String appName;
    protected DeploymentSlotSetting deploymentSlotSetting;
    protected String resourceGroup;
    protected Region region;
    protected String pricingTier;
    protected String servicePlanName;
    protected String servicePlanResourceGroup;
    protected OperatingSystem os;

    protected JavaVersion javaVersion;
    protected WebContainer webContainer;
    protected Settings mavenSettings;
    protected String image;
    protected String serverId;
    protected String registryUrl;
    protected String schemaVersion;

    // web app runtime related configurations
    protected List<DeploymentResource> resources;
    protected String stagingDirectoryPath;
    protected String buildDirectoryAbsolutePath;
    protected MavenProject project;
    protected MavenSession session;
    protected MavenResourcesFiltering filtering;

    public String getRegionOrDefault() {
        return region != null ? region.toString() : DEFAULT_REGION.toString();
    }

    public String getJavaVersionOrDefault() {
        return JavaVersionUtils.formatJavaVersion(Objects.nonNull(javaVersion) ? javaVersion : DEFAULT_JAVA_VERSION);
    }

    public String getWebContainerOrDefault() {
        return WebContainerUtils.formatWebContainer(Objects.nonNull(webContainer) ? webContainer : DEFAULT_CONTAINER);
    }
}
