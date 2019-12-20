/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

import java.util.Map;

public class SettingsHandlerImpl implements SettingsHandler {
    private AbstractWebAppMojo mojo;

    public SettingsHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void processSettings(WithCreate withCreate) throws AzureExecutionException {
        final Map appSettings = mojo.getAppSettings();
        if (appSettings != null && !appSettings.isEmpty()) {
            withCreate.withAppSettings(appSettings);
        }
    }

    @Override
    public void processSettings(Update update) throws AzureExecutionException {
        final Map appSettings = mojo.getAppSettings();
        if (appSettings != null && !appSettings.isEmpty()) {
            update.withAppSettings(appSettings);
        }
    }
}
