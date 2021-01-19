/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;

import java.util.Map;

public interface WebAppDeploymentSlotCreatable {

    interface WithName {
        WebAppDeploymentSlotCreatable withName(String name);
    }

    WebAppDeploymentSlotCreatable withAppSettings(Map<String, String> appSettings);

    WebAppDeploymentSlotCreatable withConfigurationSource(String source);

    WebAppDeploymentSlot create();
}
