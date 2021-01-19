/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.tools.common.model.Region;

import java.util.Map;

public interface IAppServiceCreator<T> {
    IAppServiceCreator<T> withName(String name);

    IAppServiceCreator<T> withSubscription(String subscriptionId);

    IAppServiceCreator<T> withResourceGroup(String resourceGroupName);

    IAppServiceCreator<T> withAppServicePlan(String appServicePlanId);

    IAppServiceCreator<T> withAppServicePlan(String resourceGroup, String planName);

    IAppServiceCreator<T> withRuntime(Runtime runtime);

    IAppServiceCreator<T> withDockerConfiguration(DockerConfiguration dockerConfiguration);

    IAppServiceCreator<T> withAppSettings(Map<String, String> appSettings);

    T create();
}
