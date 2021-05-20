/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;

import java.util.Map;

public interface IFunctionAppDeploymentSlot extends IFunctionAppBase<FunctionAppDeploymentSlotEntity> {
    IFunctionApp functionApp();

    Creator create();

    Updater update();

    FunctionAppDeploymentSlotEntity entity();

    interface Creator {
        Creator withName(String name);

        Creator withAppSettings(Map<String, String> appSettings);

        Creator withConfigurationSource(String source);

        Creator withDiagnosticConfig(DiagnosticConfig diagnosticConfig);

        IFunctionAppDeploymentSlot commit();
    }

    interface Updater {
        Updater withoutAppSettings(String key);

        Updater withAppSettings(Map<String, String> appSettings);

        Updater withDiagnosticConfig(DiagnosticConfig diagnosticConfig);

        IFunctionAppDeploymentSlot commit();
    }
}
