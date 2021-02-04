/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
public abstract class AbstractAppServiceUpdater<T> implements IAppServiceUpdater {
    private Optional<DockerConfiguration> dockerConfiguration = null;
    private Optional<Runtime> runtime = null;
    private Optional<PricingTier> pricingTier = null;
    private Optional<AppServicePlanEntity> appServicePlan = null;
    private Optional<Map<String, String>> appSettings = null;

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
    public IAppServiceUpdater<T> withAppSettings(Map appSettings) {
        this.appSettings = Optional.ofNullable(appSettings);
        return this;
    }
}
