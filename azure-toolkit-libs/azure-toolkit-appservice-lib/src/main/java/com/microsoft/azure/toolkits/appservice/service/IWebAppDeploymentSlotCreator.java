/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.service;

import java.util.Map;

public interface IWebAppDeploymentSlotCreator {

    IWebAppDeploymentSlotCreator withName(String name);

    IWebAppDeploymentSlotCreator withAppSettings(Map<String, String> appSettings);

    IWebAppDeploymentSlotCreator withConfigurationSource(String source);

    IWebAppDeploymentSlot create();
}
