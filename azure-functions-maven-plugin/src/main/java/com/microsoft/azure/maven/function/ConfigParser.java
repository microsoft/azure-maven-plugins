/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class ConfigParser {
    private final AbstractFunctionMojo mojo;

    public ConfigParser(AbstractFunctionMojo mojo) {
        this.mojo = mojo;
    }

    public FunctionAppConfig parseConfig() throws AzureExecutionException {
        return (FunctionAppConfig) new FunctionAppConfig()
                .disableAppInsights(mojo.isDisableAppInsights())
                .appInsightsKey(mojo.getAppInsightsKey())
                .appInsightsInstance(mojo.getAppInsightsInstance())
                .subscriptionId(mojo.getSubscriptionId())
                .resourceGroup(mojo.getResourceGroup())
                .appName(mojo.getAppName())
                .servicePlanName(mojo.getAppServicePlanName())
                .servicePlanResourceGroup(mojo.getAppServicePlanResourceGroup())
                .deploymentSlotName(getDeploymentSlotName())
                .deploymentSlotConfigurationSource(getDeploymentSlotConfigurationSource())
                .pricingTier(getParsedPricingTier())
                .region(getParsedRegion())
                .runtime(getRuntimeConfig())
                .appSettings(mojo.getAppSettings());
    }

    public RuntimeConfig getRuntimeConfig() throws AzureExecutionException {
        final RuntimeConfiguration runtime = mojo.getRuntimeConfiguration();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = Optional.ofNullable(runtime.getOs()).map(OperatingSystem::fromString).orElse(null);
        final JavaVersion javaVersion = Optional.ofNullable(runtime.getJavaVersion()).map(JavaVersion::fromString).orElse(null);
        final RuntimeConfig result = new RuntimeConfig().os(os).javaVersion(javaVersion).webContainer(WebContainer.JAVA_OFF)
                .image(runtime.getImage()).registryUrl(runtime.getRegistryUrl());
        if (StringUtils.isNotEmpty(runtime.getServerId())) {
            final MavenDockerCredentialProvider credentialProvider = MavenDockerCredentialProvider.fromMavenSettings(mojo.getSettings(), runtime.getServerId());
            result.username(credentialProvider.getUsername()).password(credentialProvider.getPassword());
        }
        return result;
    }

    private String getDeploymentSlotName() {
        return Optional.ofNullable(mojo.getDeploymentSlotSetting()).map(DeploymentSlotSetting::getName).orElse(null);
    }

    private String getDeploymentSlotConfigurationSource() {
        return Optional.ofNullable(mojo.getDeploymentSlotSetting()).map(DeploymentSlotSetting::getConfigurationSource).orElse(null);
    }

    private Region getParsedRegion() {
        return Optional.ofNullable(mojo.getRegion()).map(Region::fromName).orElse(null);
    }

    private PricingTier getParsedPricingTier() {
        return Optional.ofNullable(mojo.getPricingTier()).map(PricingTier::fromString).orElse(null);
    }
}
