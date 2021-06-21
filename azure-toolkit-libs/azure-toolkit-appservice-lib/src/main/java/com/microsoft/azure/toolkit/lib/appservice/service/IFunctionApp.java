/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;

import java.util.List;
import java.util.Map;

public interface IFunctionApp extends IFunctionAppBase<FunctionAppEntity> {
    FunctionAppEntity entity();

    IAppServicePlan plan();

    IAppServiceCreator<? extends IFunctionApp> create();

    IAppServiceUpdater<? extends IFunctionApp> update();

    IFunctionAppDeploymentSlot deploymentSlot(String slotName);

    List<IFunctionAppDeploymentSlot> deploymentSlots(boolean... force);

    List<FunctionEntity> listFunctions(boolean... force);

    Map<String, String> listFunctionKeys(String functionName);

    void triggerFunction(String functionName, Object input);

    void swap(String slotName);

    void syncTriggers();
}
