/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;

import java.util.Map;

public interface IWebAppDeploymentSlot extends IWebAppBase<WebAppDeploymentSlotEntity> {
    IWebApp webApp();

    Creator create();

    Updater update();

    WebAppDeploymentSlotEntity entity();

    interface Creator {

        Creator withName(String name);

        Creator withAppSettings(Map<String, String> appSettings);

        Creator withConfigurationSource(String source);

        Creator withDiagnosticConfig(DiagnosticConfig diagnosticConfig);

        IWebAppDeploymentSlot commit();
    }

    interface Updater {
        Updater withoutAppSettings(String key);

        Updater withAppSettings(Map<String, String> appSettings);

        Updater withDiagnosticConfig(DiagnosticConfig diagnosticConfig);

        IWebAppDeploymentSlot commit();
    }
}
