/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;

public abstract class BaseRuntimeHandler implements RuntimeHandler {
    protected RuntimeStack runtime;
    protected JavaVersion javaVersion;
    protected WebContainer webContainer;
    protected String appName;
    protected String resourceGroup;
    protected Region region;
    protected PricingTier pricingTier;
    protected String servicePlanName;
    protected String servicePlanResourceGroup;
    protected Azure azure;
    protected Settings settings;
    protected String image;
    protected String serverId;
    protected String registryUrl;
    protected Log log;

    public abstract static class Builder<T extends Builder<T>> {
        private RuntimeStack runtime;
        protected JavaVersion javaVersion;
        protected WebContainer webContainer;
        private String appName;
        private String resourceGroup;
        private Region region;
        private PricingTier pricingTier;
        private String servicePlanName;
        private String servicePlanResourceGroup;
        private Azure azure;
        private Settings settings;
        protected String image;
        protected String serverId;
        protected String registryUrl;
        private Log log;

        public T runtime(final RuntimeStack value) {
            this.runtime = value;
            return self();
        }

        public T appName(final String value) {
            this.appName = value;
            return self();
        }

        public T resourceGroup(final String value) {
            this.resourceGroup = value;
            return self();
        }

        public T region(final Region value) {
            this.region = value;
            return self();
        }

        public T pricingTier(final PricingTier value) {
            this.pricingTier = value;
            return self();
        }

        public T servicePlanName(final String value) {
            this.servicePlanName = value;
            return self();
        }

        public T servicePlanResourceGroup(final String value) {
            this.servicePlanResourceGroup = value;
            return self();
        }

        public T azure(final Azure value) {
            this.azure = value;
            return self();
        }

        public T mavenSettings(final Settings value) {
            this.settings = value;
            return self();
        }

        public T log(final Log value) {
            this.log = value;
            return self();
        }

        public T image(final String value) {
            this.image = value;
            return self();
        }

        public T serverId(final String value) {
            this.serverId = value;
            return self();
        }

        public T registryUrl(final String value) {
            this.registryUrl = value;
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

        public abstract BaseRuntimeHandler build();

        protected abstract T self();
    }

    @Override
    public AppServicePlan updateAppServicePlan(final WebApp app) throws Exception {
        final AppServicePlan appServicePlan = WebAppUtils.getAppServicePlanByWebApp(app);
        final AppServicePlan.Update appServicePlanUpdate = appServicePlan.update();
        // Update pricing tier
        if (pricingTier != null && appServicePlan.pricingTier().equals(pricingTier)) {
            appServicePlanUpdate.withPricingTier(pricingTier);
        }
        return appServicePlanUpdate.apply();
    }

    protected BaseRuntimeHandler(Builder<?> builder) {
        this.runtime = builder.runtime;
        this.javaVersion = builder.javaVersion;
        this.webContainer = builder.webContainer;
        this.appName = builder.appName;
        this.resourceGroup = builder.resourceGroup;
        this.region = builder.region;
        this.pricingTier = builder.pricingTier;
        this.servicePlanName = builder.servicePlanName;
        this.servicePlanResourceGroup = builder.servicePlanResourceGroup;
        this.azure = builder.azure;
        this.settings = builder.settings;
        this.image = builder.image;
        this.serverId = builder.serverId;
        this.registryUrl = builder.registryUrl;
        this.log = builder.log;
    }

}
