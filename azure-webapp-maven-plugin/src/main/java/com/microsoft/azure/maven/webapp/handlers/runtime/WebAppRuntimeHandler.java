/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.handlers.runtime.BaseRuntimeHandler;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class WebAppRuntimeHandler extends BaseRuntimeHandler<WebApp> {
    protected RuntimeStack runtime;
    protected JavaVersion javaVersion;
    protected WebContainer webContainer;

    public abstract static class Builder<T extends WebAppRuntimeHandler.Builder<T>> extends BaseRuntimeHandler.Builder<T> {
        protected RuntimeStack runtime;
        protected JavaVersion javaVersion;
        protected WebContainer webContainer;

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

        public abstract WebAppRuntimeHandler build();

        protected abstract T self();

    }

    protected WebAppRuntimeHandler(Builder<?> builder) {
        super(builder);
        this.runtime = builder.runtime;
        this.javaVersion = builder.javaVersion;
        this.webContainer = builder.webContainer;
    }

    @Override
    public abstract WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws MojoExecutionException;

    @Override
    public abstract WebApp.Update updateAppRuntime(WebApp app) throws MojoExecutionException;

    protected abstract OperatingSystem getAppServicePlatform();

    @Override
    protected void changeAppServicePlan(WebApp app, AppServicePlan appServicePlan) {
        app.update().withExistingAppServicePlan(appServicePlan).apply();
    }

    protected AppServicePlan createOrGetAppServicePlan() throws MojoExecutionException {
        return WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
                servicePlanResourceGroup, region, getPricingTierOrDefault(), log, getAppServicePlatform());
    }

    protected PricingTier getPricingTierOrDefault(){
        return pricingTier == null ? WebAppConfiguration.DEFAULT_PRICINGTIER : pricingTier;
    }
}
