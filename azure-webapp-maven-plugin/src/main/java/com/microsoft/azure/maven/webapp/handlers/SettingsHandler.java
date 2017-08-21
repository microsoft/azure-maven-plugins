/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import org.apache.maven.plugin.MojoExecutionException;

public interface SettingsHandler {
    void processSettings(final WithCreate withCreate) throws MojoExecutionException;

    void processSettings(final Update update) throws MojoExecutionException;
}
