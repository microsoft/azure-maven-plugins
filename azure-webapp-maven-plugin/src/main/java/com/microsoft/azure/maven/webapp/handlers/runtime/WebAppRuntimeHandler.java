/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.handlers.runtime.BaseRuntimeHandler;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

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

    @Override
    public AppServicePlan updateAppServicePlan(final WebApp app) throws Exception {
        final AppServicePlan appServicePlan = WebAppUtils.getAppServicePlanByWebApp(app);
        // If app's service plan differs from pom, change and update it
        if ((StringUtils.isNotEmpty(servicePlanName) && !servicePlanName.equals(appServicePlan.name())) ||
                (StringUtils.isNotEmpty(servicePlanResourceGroup) &&
                        !servicePlanResourceGroup.equals(appServicePlan.resourceGroupName()))) {
            final AppServicePlan newAppServicePlan = createOrGetAppServicePlan();
            app.update().withExistingAppServicePlan(newAppServicePlan).apply();
            return WebAppUtils.updateAppServicePlan(newAppServicePlan, pricingTier, log);
        } else {
            return WebAppUtils.updateAppServicePlan(appServicePlan, null, log);
        }
    }

    protected abstract OperatingSystem getAppServicePlatform();

    protected WebAppRuntimeHandler(Builder<?> builder) {
        super(builder);
        this.runtime = builder.runtime;
        this.javaVersion = builder.javaVersion;
        this.webContainer = builder.webContainer;
    }

    protected AppServicePlan createOrGetAppServicePlan() throws MojoExecutionException {
        return WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azure,
                servicePlanResourceGroup, region, pricingTier, log, getAppServicePlatform());
    }
}
