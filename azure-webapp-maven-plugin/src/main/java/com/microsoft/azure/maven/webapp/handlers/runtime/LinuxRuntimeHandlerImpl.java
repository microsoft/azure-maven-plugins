/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;

public class LinuxRuntimeHandlerImpl extends BaseRuntimeHandler {
    public static class Builder extends BaseRuntimeHandler.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public LinuxRuntimeHandlerImpl build() {
            return new LinuxRuntimeHandlerImpl(this);
        }
    }

    private LinuxRuntimeHandlerImpl(final Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan plan = createOrGetAppServicePlan();
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withBuiltInImage(runtime);
    }

    @Override
    public Update updateAppRuntime(WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);
        return app.update().withBuiltInImage(runtime);
    }

    @Override
    protected OperatingSystem getAppServicePlatform() {
        return OperatingSystem.LINUX;
    }
}
