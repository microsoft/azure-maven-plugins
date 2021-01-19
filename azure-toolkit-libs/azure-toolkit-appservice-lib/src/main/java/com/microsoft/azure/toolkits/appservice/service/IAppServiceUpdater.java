/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.Runtime;

import java.util.Map;

public interface IAppServiceUpdater<T> {
    IAppServiceUpdater<T> withAppServicePlan(String appServicePlanId);

    IAppServiceUpdater<T> withAppServicePlan(String resourceGroup, String planName);

    IAppServiceUpdater<T> withRuntime(Runtime runtime);

    IAppServiceUpdater<T> withDockerConfiguration(DockerConfiguration dockerConfiguration);

    IAppServiceUpdater<T> withAppSettings(Map<String, String> appSettings);

    T apply();
}
