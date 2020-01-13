/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;

public class PrivateRegistryRuntimeHandlerImpl extends WebAppRuntimeHandler {
    protected IDockerCredentialProvider dockerCredentialProvider;

    public static class Builder extends WebAppRuntimeHandler.Builder<Builder> {
        protected IDockerCredentialProvider dockerCredentialProvider;

        @Override
        protected PrivateRegistryRuntimeHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public PrivateRegistryRuntimeHandlerImpl build() {
            return new PrivateRegistryRuntimeHandlerImpl(this);
        }

        public Builder dockerCredentialProvider(IDockerCredentialProvider value) {
            this.dockerCredentialProvider = value;
            return self();
        }
    }

    private PrivateRegistryRuntimeHandlerImpl(final Builder builder) {
        super(builder);
        this.dockerCredentialProvider = builder.dockerCredentialProvider;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException {
        final AppServicePlan plan = createOrGetAppServicePlan();
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withPrivateRegistryImage(image, registryUrl)
            .withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws AzureExecutionException {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);

        return app.update()
            .withPrivateRegistryImage(image, registryUrl)
            .withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
    }

    @Override
    protected OperatingSystem getAppServicePlatform() {
        return OperatingSystem.LINUX;
    }
}
