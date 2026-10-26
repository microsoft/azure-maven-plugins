/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ContainerAppFunctionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class FunctionAppConfig extends AppServiceConfig {
    private String storageAccountName;
    private String storageAccountResourceGroup;
    private String environment;
    private ContainerAppsEnvironmentDraft.Config environmentConfig;
    private ContainerAppFunctionConfiguration containerConfiguration;
    private LogAnalyticsWorkspaceConfig workspaceConfig;
    private Boolean enableDistributedTracing;
    private ApplicationInsightsConfig applicationInsightsConfig;
    private FlexConsumptionConfiguration flexConsumptionConfiguration;

    @Builder.Default
    private Boolean skipEndOfLifeValidation = Boolean.TRUE;

    public String environment() {
        return Optional.ofNullable(environmentConfig).map(ContainerAppsEnvironmentDraft.Config::getName).orElse(environment);
    }

    public FunctionAppConfig environment(final String environment) {
        this.environment = environment;
        this.environmentConfig = new ContainerAppsEnvironmentDraft.Config();
        this.environmentConfig.setName(environment);
        return this;
    }

    public boolean disableAppInsights() {
        return Optional.ofNullable(applicationInsightsConfig)
            .map(c -> BooleanUtils.isTrue(c.getDisableAppInsights())).orElse(false);
    }

    @Nullable
    public String appInsightsInstance() {
        return Optional.ofNullable(applicationInsightsConfig)
            .filter(c -> BooleanUtils.isNotTrue(c.getDisableAppInsights()))
            .map(ApplicationInsightsConfig::getName).orElse(null);
    }

    @Nullable
    public String appInsightsKey() {
        return Optional.ofNullable(applicationInsightsConfig)
            .filter(c -> BooleanUtils.isNotTrue(c.getDisableAppInsights()))
            .map(ApplicationInsightsConfig::getInstrumentationKey).orElse(null);
    }

    @Nullable
    public String appInsightsConnectionString() {
        return Optional.ofNullable(applicationInsightsConfig)
            .filter(c -> BooleanUtils.isNotTrue(c.getDisableAppInsights()))
            .map(ApplicationInsightsConfig::getConnectionString).orElse(null);
    }

    @Nullable
    public LogAnalyticsWorkspaceConfig workspaceConfig() {
        return Optional.ofNullable(applicationInsightsConfig)
            .filter(c -> BooleanUtils.isNotTrue(c.getDisableAppInsights()))
            .map(ApplicationInsightsConfig::getWorkspaceConfig).orElse(null);
    }

    public FunctionAppConfig disableAppInsights(final Boolean disableAppInsights) {
        this.applicationInsightsConfig = Optional.ofNullable(applicationInsightsConfig).orElseGet(ApplicationInsightsConfig::new);
        this.applicationInsightsConfig.setDisableAppInsights(disableAppInsights);
        return this;
    }

    @Nonnull
    public FunctionAppConfig appInsightsInstance(final String appInsightsInstance) {
        this.applicationInsightsConfig = Optional.ofNullable(applicationInsightsConfig).orElseGet(ApplicationInsightsConfig::new);
        this.applicationInsightsConfig.setName(appInsightsInstance);
        return this;
    }

    @Nonnull
    @Deprecated
    public FunctionAppConfig appInsightsKey(final String key) {
        this.applicationInsightsConfig = Optional.ofNullable(applicationInsightsConfig).orElseGet(ApplicationInsightsConfig::new);
        this.applicationInsightsConfig.setInstrumentationKey(key);
        return this;
    }

    @Nonnull
    public FunctionAppConfig appInsightsConnectionString(final String connectionString) {
        this.applicationInsightsConfig = Optional.ofNullable(applicationInsightsConfig).orElseGet(ApplicationInsightsConfig::new);
        this.applicationInsightsConfig.setConnectionString(connectionString);
        return this;
    }

    @Nonnull
    public FunctionAppConfig workspaceConfig(final LogAnalyticsWorkspaceConfig workspaceConfig) {
        this.applicationInsightsConfig = Optional.ofNullable(applicationInsightsConfig).orElseGet(ApplicationInsightsConfig::new);
        this.applicationInsightsConfig.setWorkspaceConfig(workspaceConfig);
        return this;
    }

    public String getStorageAccountName() {
        return storageAccountName;
    }

    public String getStorageAccountResourceGroup() {
        return storageAccountResourceGroup;
    }

    public Boolean getEnableDistributedTracing() {
        return enableDistributedTracing;
    }

    public ApplicationInsightsConfig getApplicationInsightsConfig() {
        return applicationInsightsConfig;
    }

    public FlexConsumptionConfiguration getFlexConsumptionConfiguration() {
        return flexConsumptionConfiguration;
    }

    public void setStorageAccountName(String storageAccountName) {
        this.storageAccountName = storageAccountName;
    }

    public void setStorageAccountResourceGroup(String storageAccountResourceGroup) {
        this.storageAccountResourceGroup = storageAccountResourceGroup;
    }

    public void setEnableDistributedTracing(Boolean enableDistributedTracing) {
        this.enableDistributedTracing = enableDistributedTracing;
    }

    public void setApplicationInsightsConfig(ApplicationInsightsConfig applicationInsightsConfig) {
        this.applicationInsightsConfig = applicationInsightsConfig;
    }

    public void setFlexConsumptionConfiguration(FlexConsumptionConfiguration flexConsumptionConfiguration) {
        this.flexConsumptionConfiguration = flexConsumptionConfiguration;
    }

    public void setSkipEndOfLifeValidation(Boolean skipEndOfLifeValidation) {
        this.skipEndOfLifeValidation = skipEndOfLifeValidation;
    }

    public Boolean getSkipEndOfLifeValidation() {
        return skipEndOfLifeValidation;
    }
}
