/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;

public interface RuntimeHandler {
    WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws Exception;

    WebApp.Update updateAppRuntime() throws Exception;
}
