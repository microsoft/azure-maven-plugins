/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;

import java.util.List;

public interface WebAppDeploymentSlotsManager {
    WebAppDeploymentSlotCreatable.WithName create();

    WebAppDeploymentSlot getById(String id);

    WebAppDeploymentSlot getByName(String slotName);

    List<WebAppDeploymentSlot> list();
}
