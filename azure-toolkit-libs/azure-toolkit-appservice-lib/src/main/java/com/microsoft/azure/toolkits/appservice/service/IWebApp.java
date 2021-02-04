/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;

import java.util.List;

public interface IWebApp extends IAppService {
    WebAppEntity entity();

    IAppServicePlan plan();

    IAppServiceCreator<? extends IWebApp> create();

    IAppServiceUpdater<? extends IWebApp> update();

    IWebAppDeploymentSlot deploymentSlot(String slotName);

    List<IWebAppDeploymentSlot> deploymentSlots();
}
