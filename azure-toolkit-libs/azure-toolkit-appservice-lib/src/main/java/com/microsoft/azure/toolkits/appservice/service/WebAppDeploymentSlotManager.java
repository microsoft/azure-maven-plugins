/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;

public interface WebAppDeploymentSlotManager {
    void start();

    void stop();

    void restart();

    void delete();

    boolean exists();

    WebAppDeploymentSlot get();
}
