/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import org.apache.maven.plugin.MojoExecutionException;

public interface SettingsHandler {
    void processSettings(final WebApp.DefinitionStages.WithCreate withCreate) throws MojoExecutionException;

    void processSettings(final WebApp.Update update) throws MojoExecutionException;
}
