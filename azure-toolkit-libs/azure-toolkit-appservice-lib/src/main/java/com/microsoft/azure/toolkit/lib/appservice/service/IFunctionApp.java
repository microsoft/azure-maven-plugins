/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;

import java.util.List;

public interface IFunctionApp extends IFunctionAppBase{
    FunctionAppEntity entity();

    IAppServicePlan plan();

    IAppServiceCreator<? extends IFunctionApp> create();

    IAppServiceUpdater<? extends IFunctionApp> update();

    IFunctionAppDeploymentSlot deploymentSlot(String slotName);

    List<IFunctionAppDeploymentSlot> deploymentSlots();

    List<FunctionEntity> listFunctions();

    void triggerFunction(String functionName);

    void swap(String slotName);
}
