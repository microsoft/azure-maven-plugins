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
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;

public class PrivateRegistryRuntimeHandlerImpl extends WebAppRuntimeHandler {
    public static class Builder extends WebAppRuntimeHandler.Builder<Builder> {

        @Override
        protected PrivateRegistryRuntimeHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public PrivateRegistryRuntimeHandlerImpl build() {
            return new PrivateRegistryRuntimeHandlerImpl(this);
        }
    }

    private PrivateRegistryRuntimeHandlerImpl(final Builder builder) {
        super(builder);
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException {
        checkServerConfiguration(DockerImageType.PRIVATE_REGISTRY, dockerCredentialProvider);

        final AppServicePlan plan = createOrGetAppServicePlan();
        return WebAppUtils.defineLinuxApp(resourceGroup, appName, azure, plan)
            .withPrivateRegistryImage(image, registryUrl)
            .withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
    }

    @Override
    public WebApp.Update updateAppRuntime(final WebApp app) throws AzureExecutionException {
        WebAppUtils.assureLinuxWebApp(app);
        WebAppUtils.clearTags(app);

        checkServerConfiguration(DockerImageType.PRIVATE_REGISTRY, dockerCredentialProvider);

        return app.update()
            .withPrivateRegistryImage(image, registryUrl)
            .withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
    }

    @Override
    protected OperatingSystem getAppServicePlatform() {
        return OperatingSystem.LINUX;
    }
}
