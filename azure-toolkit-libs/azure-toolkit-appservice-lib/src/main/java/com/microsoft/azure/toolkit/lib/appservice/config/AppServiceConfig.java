/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode
public class AppServiceConfig {

    private String subscriptionId;

    private String resourceGroup;

    private Region region;

    private PricingTier pricingTier;

    private String appName;

    private String servicePlanResourceGroup;

    private String servicePlanName;

    private RuntimeConfig runtime;

    private Map<String, String> appSettings;

    private Set<String> appSettingsToRemove;

    private DeploymentSlotConfig slotConfig;

    private DiagnosticConfig diagnosticConfig;

    private File file;

    public String deploymentSlotName() {
        return Optional.ofNullable(slotConfig).map(DeploymentSlotConfig::getName).orElse(null);
    }

    public AppServiceConfig deploymentSlotName(String deploymentSlotName) {
        this.slotConfig = Optional.ofNullable(this.slotConfig).orElseGet(DeploymentSlotConfig::new);
        this.slotConfig.setName(deploymentSlotName);
        return this;
    }

    public String deploymentSlotConfigurationSource() {
        return Optional.ofNullable(slotConfig).map(DeploymentSlotConfig::getConfigurationSource).orElse(null);
    }

    public AppServiceConfig deploymentSlotConfigurationSource(String source) {
        this.slotConfig = Optional.ofNullable(this.slotConfig).orElseGet(DeploymentSlotConfig::new);
        this.slotConfig.setConfigurationSource(source);
        return this;
    }

    public static AppServicePlanConfig getServicePlanConfig(@Nonnull final AppServiceConfig config) {
        return AppServicePlanConfig.builder()
            .subscriptionId(config.subscriptionId())
            .resourceGroupName(StringUtils.firstNonBlank(config.servicePlanResourceGroup(), config.resourceGroup()))
            .name(config.servicePlanName())
            .region(config.region())
            .os(Optional.ofNullable(config.runtime).map(RuntimeConfig::os).orElse(null))
            .pricingTier(config.pricingTier())
            .build();
    }

    @Nullable
    public static ResourceGroup getResourceGroup(@Nonnull final AppServiceConfig config) {
        if (StringUtils.isAnyBlank(config.getSubscriptionId(), config.getResourceGroup())) {
            return null;
        }
        final ResourceGroup rg = Azure.az(AzureResources.class).groups(config.subscriptionId())
            .getOrDraft(config.resourceGroup(), config.resourceGroup());
        if (rg.isDraftForCreating()) {
            final ResourceGroupDraft draft = (ResourceGroupDraft) rg;
            draft.setRegion(config.region());
            return draft;
        }
        return rg;
    }

    public static AppServiceConfig buildDefaultWebAppConfig(String resourceGroup, String appName, String packaging) {
        final WebAppRuntime defaultRuntime = StringUtils.equalsIgnoreCase(packaging, "war") ? WebAppRuntime.getDefaultTomcatRuntime() :
            StringUtils.equalsIgnoreCase(packaging, "ear") ? WebAppRuntime.getDefaultJbossRuntime() : WebAppRuntime.getDefaultJavaseRuntime();
        final RuntimeConfig runtimeConfig = new RuntimeConfig()
            .os(defaultRuntime.getOperatingSystem())
            .webContainer(defaultRuntime.getContainerUserText())
            .javaVersion(defaultRuntime.getJavaVersionUserText());
        final AppServiceConfig appServiceConfig = buildDefaultAppServiceConfig(resourceGroup, appName);
        appServiceConfig.runtime(runtimeConfig);
        return appServiceConfig;
    }

    public static FunctionAppConfig buildDefaultFunctionConfig(String resourceGroup, String appName) {
        final FunctionAppConfig result = new FunctionAppConfig();
        final AppServiceConfig appServiceConfig = buildDefaultAppServiceConfig(resourceGroup, appName);
        AppServiceConfigUtils.mergeAppServiceConfig(result, appServiceConfig);
        final FunctionAppRuntime defaultRuntime = FunctionAppRuntime.getDefault();
        RuntimeConfig runtimeConfig = new RuntimeConfig()
            .os(defaultRuntime.getOperatingSystem())
            .javaVersion(defaultRuntime.getJavaVersionUserText());
        result.runtime(runtimeConfig);
        result.pricingTier(PricingTier.CONSUMPTION);
        result.flexConsumptionConfiguration(FlexConsumptionConfiguration.DEFAULT_CONFIGURATION);
        return result;
    }

    @Nonnull
    private static AppServiceConfig buildDefaultAppServiceConfig(String resourceGroup, String appName) {
        final AppServiceConfig appServiceConfig = new AppServiceConfig();
        appServiceConfig.region(Region.US_CENTRAL);

        appServiceConfig.resourceGroup(resourceGroup);
        appServiceConfig.appName(appName);
        appServiceConfig.servicePlanResourceGroup(resourceGroup);
        appServiceConfig.servicePlanName(String.format("asp-%s", appName));
        return appServiceConfig;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Region getRegion() {
        return region;
    }

    public PricingTier getPricingTier() {
        return pricingTier;
    }

    public String getAppName() {
        return appName;
    }

    public String getServicePlanResourceGroup() {
        return servicePlanResourceGroup;
    }

    public String getServicePlanName() {
        return servicePlanName;
    }

    public RuntimeConfig getRuntime() {
        return runtime;
    }

    public Map<String, String> getAppSettings() {
        return appSettings;
    }

    public Set<String> getAppSettingsToRemove() {
        return appSettingsToRemove;
    }

    public DeploymentSlotConfig getSlotConfig() {
        return slotConfig;
    }

    public DiagnosticConfig getDiagnosticConfig() {
        return diagnosticConfig;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setPricingTier(PricingTier pricingTier) {
        this.pricingTier = pricingTier;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setServicePlanResourceGroup(String servicePlanResourceGroup) {
        this.servicePlanResourceGroup = servicePlanResourceGroup;
    }

    public void setServicePlanName(String servicePlanName) {
        this.servicePlanName = servicePlanName;
    }

    public void setRuntime(RuntimeConfig runtime) {
        this.runtime = runtime;
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.appSettings = appSettings;
    }

    public void setAppSettingsToRemove(Set<String> appSettingsToRemove) {
        this.appSettingsToRemove = appSettingsToRemove;
    }

    public void setSlotConfig(DeploymentSlotConfig slotConfig) {
        this.slotConfig = slotConfig;
    }

    public void setDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
        this.diagnosticConfig = diagnosticConfig;
    }
}
