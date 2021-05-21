/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public abstract class AbstractAppServiceUpdater<T> implements IAppServiceUpdater {
    private Optional<DiagnosticConfig> diagnosticConfig = null;
    private Optional<DockerConfiguration> dockerConfiguration = null;
    private Optional<Runtime> runtime = null;
    private Optional<PricingTier> pricingTier = null;
    private Optional<AppServicePlanEntity> appServicePlan = null;
    private final List<String> appSettingsToRemove = new ArrayList<>();
    private final Map<String, String> appSettingsToAdd = new HashMap<>();

    @Override
    public IAppServiceUpdater withPlan(String appServicePlanId) {
        appServicePlan = Optional.of(AppServicePlanEntity.builder().id(appServicePlanId).build());
        return this;
    }

    @Override
    public IAppServiceUpdater withPlan(String resourceGroup, String planName) {
        appServicePlan = Optional.of(AppServicePlanEntity.builder().resourceGroup(resourceGroup).name(planName).build());
        return this;
    }

    @Override
    public IAppServiceUpdater<T> withRuntime(Runtime runtime) {
        this.runtime = Optional.ofNullable(runtime);
        return this;
    }

    @Override
    public IAppServiceUpdater withDockerConfiguration(DockerConfiguration dockerConfiguration) {
        this.dockerConfiguration = Optional.ofNullable(dockerConfiguration);
        return this;
    }

    @Override
    public IAppServiceUpdater withoutAppSettings(String key) {
        this.appSettingsToRemove.add(key);
        return this;
    }

    @Override
    public IAppServiceUpdater<T> withAppSettings(Map appSettings) {
        this.appSettingsToAdd.putAll(appSettings);
        return this;
    }

    @Override
    public IAppServiceUpdater<T> withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
        this.diagnosticConfig = Optional.ofNullable(diagnosticConfig);
        return this;
    }
}
