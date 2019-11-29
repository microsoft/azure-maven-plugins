/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import org.apache.maven.plugin.MojoExecutionException;

public class WindowsRuntimeHandlerImpl extends WebAppRuntimeHandler {
    public static class Builder extends WebAppRuntimeHandler.Builder<WindowsRuntimeHandlerImpl.Builder> {
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
    public WithCreate defineAppWithRuntime() throws MojoExecutionException {
        final AppServicePlan plan = createOrGetAppServicePlan();
        final WithCreate withCreate = WebAppUtils.defineWindowsApp(resourceGroup, appName, azure, plan);
        withCreate.withJavaVersion(javaVersion).withWebContainer(webContainer);
        return withCreate;
    }

    @Override
    public Update updateAppRuntime(final WebApp app) throws MojoExecutionException {
        WebAppUtils.assureWindowsWebApp(app);
        WebAppUtils.clearTags(app);
        final Update update = app.update();
        update.withJavaVersion(javaVersion).withWebContainer(webContainer);
        return update;
    }

    @Override
    protected OperatingSystem getAppServicePlatform() {
        return OperatingSystem.WINDOWS;
    }
}
