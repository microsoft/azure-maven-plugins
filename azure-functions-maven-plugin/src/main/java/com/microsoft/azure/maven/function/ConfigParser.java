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
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
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
        final RuntimeConfiguration runtimeConfig = mojo.getRuntimeConfiguration();
        if (runtimeConfig == null) {
            return null;
        }
        final OperatingSystem os = Optional.ofNullable(runtimeConfig.getOs()).map(OperatingSystem::fromString)
                .orElseGet(() -> Optional.ofNullable(getServicePlan()).map(AppServicePlan::getOperatingSystem).orElse(OperatingSystem.LINUX));
        FunctionAppRuntime runtime = null;
        if (os == OperatingSystem.DOCKER) {
            runtime = FunctionAppRuntime.DOCKER;
        } else if (os == OperatingSystem.LINUX) {
            runtime = FunctionAppLinuxRuntime.fromJavaVersionUserText(runtimeConfig.getJavaVersion());
        } else if (os == OperatingSystem.WINDOWS) {
            runtime = FunctionAppWindowsRuntime.fromJavaVersionUserText(runtimeConfig.getJavaVersion());
        }
        if (Objects.isNull(runtime) && !StringUtils.isAllBlank(runtimeConfig.getOs(), runtimeConfig.getJavaVersion())) {
            throw new AzureToolkitRuntimeException("invalid runtime configuration, please refer to https://aka.ms/maven_function_configuration#supported-runtime for valid values");
        }
        final RuntimeConfig result = new RuntimeConfig().runtime(runtime)
                .image(runtimeConfig.getImage()).registryUrl(runtimeConfig.getRegistryUrl());
        if (StringUtils.isNotEmpty(runtimeConfig.getServerId())) {
            final MavenDockerCredentialProvider credentialProvider = MavenDockerCredentialProvider.fromMavenSettings(mojo.getSettings(), runtimeConfig.getServerId());
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
