/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.maven.handlers.RuntimeHandler;

public class BaseRuntimeHandler<T extends WebAppBase> implements RuntimeHandler<T> {
    @Override
    public WebAppBase.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        return null;
    }

    @Override
    public WebAppBase.Update updateAppRuntime(T app) throws Exception {
        return null;
    }

    @Override
    public AppServicePlan updateAppServicePlan(T app) throws Exception {
        return null;
    }
}
