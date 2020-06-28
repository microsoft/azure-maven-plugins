/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;

public class PublicDockerHubRuntimeHandlerImpl extends WebAppRuntimeHandler {
    public static class Builder extends WebAppRuntimeHandler.Builder<PublicDockerHubRuntimeHandlerImpl.Builder> {
        @Override
        protected PublicDockerHubRuntimeHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public PublicDockerHubRuntimeHandlerImpl build() {
            return new PublicDockerHubRuntimeHandlerImpl(this);
        }
    }

    private PublicDockerHubRuntimeHandlerImpl(final PublicDockerHubRuntimeHandlerImpl.Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException {
        final AppServicePlan plan = createOrGetAppServicePlan();
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withPublicDockerHubImage(image);
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws AzureExecutionException {
        WebAppUtils.assureLinuxWebApp(app);
        return app.update().withPublicDockerHubImage(image);
    }

    @Override
    protected OperatingSystem getAppServicePlatform() {
        return OperatingSystem.LINUX;
    }
}
