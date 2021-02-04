/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.entity.WebAppDeploymentSlotEntity;

public interface IWebAppDeploymentSlot extends IAppService {
    IWebApp webApp();

    IWebAppDeploymentSlotCreator create();

    WebAppDeploymentSlotEntity entity();
}
