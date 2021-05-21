/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;

import java.util.List;

public interface IWebApp extends IWebAppBase<WebAppEntity> {
    WebAppEntity entity();

    IAppServicePlan plan();

    IAppServiceCreator<? extends IWebApp> create();

    IAppServiceUpdater<? extends IWebApp> update();

    IWebAppDeploymentSlot deploymentSlot(String slotName);

    List<IWebAppDeploymentSlot> deploymentSlots();

    void swap(String slotName);
}
