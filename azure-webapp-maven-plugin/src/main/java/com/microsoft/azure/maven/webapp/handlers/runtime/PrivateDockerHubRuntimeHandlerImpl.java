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
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;


public class PrivateDockerHubRuntimeHandlerImpl extends WebAppRuntimeHandler {
    public static class Builder extends WebAppRuntimeHandler.Builder<Builder> {
        @Override
        protected PrivateDockerHubRuntimeHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public PrivateDockerHubRuntimeHandlerImpl build() {
            return new PrivateDockerHubRuntimeHandlerImpl(this);
        }
    }

    private PrivateDockerHubRuntimeHandlerImpl(final Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException {
        final MavenDockerCredentialProvider provider = MavenDockerCredentialProvider.fromMavenSettings(settings, serverId);

        final AppServicePlan plan = createOrGetAppServicePlan();
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withPrivateDockerHubImage(image)
            .withCredentials(provider.getUsername(), provider.getPassword());
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws AzureExecutionException {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);

        final MavenDockerCredentialProvider provider = MavenDockerCredentialProvider.fromMavenSettings(settings, serverId);
        return app.update()
            .withPrivateDockerHubImage(image)
            .withCredentials(provider.getUsername(), provider.getPassword());
    }

    @Override
    protected OperatingSystem getAppServicePlatform() {
        return OperatingSystem.LINUX;
    }
}
