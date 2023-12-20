/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
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
    private static final Region DEFAULT_REGION = Region.US_CENTRAL;

    public static final PricingTier DEFAULT_JBOSS_PRICING_TIER = PricingTier.PREMIUM_P1V3;
    public static final PricingTier DEFAULT_PRICINGTIER = PricingTier.PREMIUM_P1V2;

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

    protected String javaVersion;
    protected String webContainer;
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
        return region == null ? getDefaultRegion().toString() : region.toString();
    }

    public static Region getDefaultRegion() {
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (az.isLoggedIn()) {
            final Subscription sub = az.account().getSelectedSubscriptions().get(0);
            final AppServiceServiceSubscription appServiceSubscription = Azure.az(AzureAppService.class).get(sub.getId(), null);
            final List<Region> regions = Objects.requireNonNull(appServiceSubscription).listSupportedRegions();
            if (regions.contains(DEFAULT_REGION)) {
                return DEFAULT_REGION;
            } else {
                return regions.get(0);
            }
        }
        return DEFAULT_REGION;
    }

    public abstract static class WebAppConfigurationBuilder<
        C extends WebAppConfiguration,
        B extends WebAppConfiguration.WebAppConfigurationBuilder<C, B>
        > {
    }
}
