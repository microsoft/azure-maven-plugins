/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.WebAppUtils;

public class WindowsRuntimeHandlerImpl extends BaseRuntimeHandler {
    public static class Builder extends BaseRuntimeHandler.Builder<WindowsRuntimeHandlerImpl.Builder>{
        @Override
        protected WindowsRuntimeHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public WindowsRuntimeHandlerImpl build() {
            return new WindowsRuntimeHandlerImpl(this);
        }
    }

    private WindowsRuntimeHandlerImpl(final WindowsRuntimeHandlerImpl.Builder builder) {
        super(builder);
    }

    @Override
    public WithCreate defineAppWithRuntime(final AppServicePlan plan) throws Exception {
        final WithCreate withCreate = WebAppUtils.defineWindowsApp(resourceGroup, appName, azure, plan);
        withCreate.withJavaVersion(javaVersion).withWebContainer(webContainer);
        return withCreate;
    }

    @Override
    public Update updateAppRuntime(final WebApp app) throws Exception {
        WebAppUtils.assureWindowsWebApp(app);
        WebAppUtils.clearTags(app);
        final Update update = app.update();
        update.withJavaVersion(javaVersion).withWebContainer(webContainer);
        return update;
    }

}
