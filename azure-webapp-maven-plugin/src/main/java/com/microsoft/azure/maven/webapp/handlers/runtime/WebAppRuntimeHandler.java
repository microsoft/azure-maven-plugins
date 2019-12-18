/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.handlers.runtime.BaseRuntimeHandler;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;

public abstract class WebAppRuntimeHandler extends BaseRuntimeHandler<WebApp> {
    protected RuntimeStack runtime;
    protected JavaVersion javaVersion;
    protected WebContainer webContainer;
    protected IDockerCredentialProvider dockerCredentialProvider;

    public abstract static class Builder<T extends WebAppRuntimeHandler.Builder<T>> extends BaseRuntimeHandler.Builder<T> {
        protected RuntimeStack runtime;
        protected JavaVersion javaVersion;
        protected WebContainer webContainer;
        protected IDockerCredentialProvider dockerCredentialProvider;

        public T runtime(final RuntimeStack value) {
            this.runtime = value;
            return self();
        }

        public T javaVersion(final JavaVersion value) {
            this.javaVersion = value;
            return self();
        }

        public T webContainer(final WebContainer value) {
            this.webContainer = value;
            return self();
        }

        public T dockerCredentialProvider(IDockerCredentialProvider value) {
       	 this.dockerCredentialProvider = value;
            return self();
       }

        public abstract WebAppRuntimeHandler build();

        protected abstract T self();

    }

    protected WebAppRuntimeHandler(Builder<?> builder) {
        super(builder);
        this.runtime = builder.runtime;
        this.javaVersion = builder.javaVersion;
        this.webContainer = builder.webContainer;
        this.dockerCredentialProvider = builder.dockerCredentialProvider;
    }

    @Override
    public abstract WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException;

    @Override
    public abstract WebApp.Update updateAppRuntime(WebApp app) throws AzureExecutionException;

    protected abstract OperatingSystem getAppServicePlatform();

    @Override
    protected void changeAppServicePlan(WebApp app, AppServicePlan appServicePlan) {
        app.update().withExistingAppServicePlan(appServicePlan).apply();
    }

    protected AppServicePlan createOrGetAppServicePlan() throws AzureExecutionException {
        return WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
                servicePlanResourceGroup, region, getPricingTierOrDefault(), getAppServicePlatform());
    }

    protected PricingTier getPricingTierOrDefault(){
        return pricingTier == null ? WebAppConfiguration.DEFAULT_PRICINGTIER : pricingTier;
    }


    protected static void checkServerConfiguration(DockerImageType imageType, IDockerCredentialProvider dockerCredentialProvider)
            throws AzureExecutionException {
        if (imageType != DockerImageType.PUBLIC_DOCKER_HUB) {
            if (dockerCredentialProvider == null) {
                throw new AzureExecutionException("Cannot get docker credential for private image.");
            }
            if (StringUtils.isEmpty(dockerCredentialProvider.getUsername())) {
                throw new AzureExecutionException("Cannot get username for private image.");
            }
            if (StringUtils.isEmpty(dockerCredentialProvider.getPassword())) {
                throw new AzureExecutionException("Cannot get password for private image.");
            }
        }
    }
}
