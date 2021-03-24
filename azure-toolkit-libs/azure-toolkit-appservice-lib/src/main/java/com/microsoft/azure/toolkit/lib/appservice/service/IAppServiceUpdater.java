/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;

import java.util.Map;

public interface IAppServiceUpdater<T> {
    IAppServiceUpdater<T> withPlan(String appServicePlanId);

    IAppServiceUpdater<T> withPlan(String resourceGroup, String planName);

    IAppServiceUpdater<T> withRuntime(Runtime runtime);

    IAppServiceUpdater<T> withDockerConfiguration(DockerConfiguration dockerConfiguration);

    IAppServiceUpdater<T> withAppSettings(Map<String, String> appSettings);

    IAppServiceUpdater<T> withDiagnosticConfig(DiagnosticConfig diagnosticConfig);

    T commit();
}
