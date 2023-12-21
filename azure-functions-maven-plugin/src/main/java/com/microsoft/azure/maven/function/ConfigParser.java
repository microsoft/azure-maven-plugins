/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.appservice.MavenDockerCredentialProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ContainerAppFunctionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
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
                .flexConsumptionConfiguration(getFlexConsumptionConfiguration())
                .disableAppInsights(mojo.isDisableAppInsights())
                .enableDistributedTracing(mojo.getEnableDistributedTracing())
                .appInsightsKey(mojo.getAppInsightsKey())
                .appInsightsInstance(mojo.getAppInsightsInstance())
                .storageAccountName(mojo.getStorageAccountName())
                .storageAccountResourceGroup(StringUtils.firstNonBlank(mojo.getStorageAccountResourceGroup(), mojo.getResourceGroup()))
                .environment(mojo.getEnvironment())
                .containerConfiguration(getContainerConfiguration())
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

    private ContainerAppFunctionConfiguration getContainerConfiguration() {
        return ContainerAppFunctionConfiguration.builder()
            .minReplicas(mojo.getMinReplicas())
            .maxReplicas(mojo.getMaxReplicas())
            .build();
    }

    public FlexConsumptionConfiguration getFlexConsumptionConfiguration() {
        return FlexConsumptionConfiguration.builder()
            .alwaysReadyInstances(mojo.getAlwaysReadyInstances())
            .instanceSize(mojo.getInstanceSize())
            .maximumInstances(mojo.getMaximumInstances()).build();
    }

    public AppServicePlan getServicePlan() {
        final String subscriptionId = mojo.getSubscriptionId();
        final String servicePlan = mojo.getAppServicePlanName();
        final String servicePlanGroup = StringUtils.firstNonBlank(mojo.getAppServicePlanResourceGroup(), mojo.getResourceGroup());
        return StringUtils.isAnyBlank(subscriptionId, servicePlan, servicePlanGroup) ? null :
                Azure.az(AzureAppService.class).plans(subscriptionId).get(servicePlan, servicePlanGroup);
    }

    public RuntimeConfig getRuntimeConfig() throws AzureExecutionException {
        final RuntimeConfiguration runtime = mojo.getRuntimeConfiguration();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = Optional.ofNullable(runtime.getOs()).map(OperatingSystem::fromString)
            .orElseGet(() -> Optional.ofNullable(getServicePlan()).map(AppServicePlan::getOperatingSystem).orElse(null));
        final String javaVersion = runtime.getJavaVersion();
        final RuntimeConfig result = new RuntimeConfig().os(os).javaVersion(javaVersion)
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
        return Optional.ofNullable(mojo.getPricingTier()).map(PricingTier::fromString)
                .orElseGet(() -> Optional.ofNullable(getServicePlan()).map(AppServicePlan::getPricingTier).orElse(null));
    }
}
