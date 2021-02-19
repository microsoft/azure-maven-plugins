/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;

import java.util.Map;

public interface IAppServiceCreator<T> {
    IAppServiceCreator<T> withName(String name);

    IAppServiceCreator<T> withResourceGroup(String resourceGroupName);

    IAppServiceCreator<T> withPlan(String appServicePlanId);

    IAppServiceCreator<T> withPlan(String resourceGroup, String planName);

    IAppServiceCreator<T> withRuntime(Runtime runtime);

    IAppServiceCreator<T> withDockerConfiguration(DockerConfiguration dockerConfiguration);

    IAppServiceCreator<T> withAppSettings(Map<String, String> appSettings);

    T commit();
}
