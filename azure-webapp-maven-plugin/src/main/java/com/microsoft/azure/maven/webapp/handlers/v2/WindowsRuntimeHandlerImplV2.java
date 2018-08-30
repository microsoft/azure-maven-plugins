/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;

public class WindowsRuntimeHandlerImplV2 implements RuntimeHandler {
    @Override
    public WithCreate defineAppWithRuntime() throws Exception {
        // todo
        throw new Exception("Unimplemented");
    }

    @Override
    public Update updateAppRuntime(final WebApp app) throws Exception {
        // todo
        throw new Exception("Unimplemented");
    }

}
