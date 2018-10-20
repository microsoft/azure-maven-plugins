/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;

public abstract class BaseRuntimeHandler implements RuntimeHandler {
    protected RuntimeSetting runtime;
    protected String appName;
    protected String resourceGroup;
    protected String region;
    protected PricingTier pricingTier;
    protected String servicePlanName;
    protected String servicePlanResourceGroup;
    protected Azure azure;
    protected Settings settings;
    protected Log log;

    public abstract static class Builder<T extends Builder<T>> {
        private RuntimeSetting runtime;
        private String appName;
        private String resourceGroup;
        private String region;
        private PricingTier pricingTier;
        private String servicePlanName;
        private String servicePlanResourceGroup;
        private Azure azure;
        private Settings settings;
        private Log log;

        public T runtime(final RuntimeSetting value) {
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

        public T region(final String value) {
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

        public T settings(final Settings value) {
            this.settings = value;
            return self();
        }

        public T log(final Log value) {
            this.log = value;
            return self();
        }

        public abstract BaseRuntimeHandler build();

        protected abstract T self();
    }

    protected BaseRuntimeHandler(Builder<?> builder) {
        this.runtime = builder.runtime;
        this.appName = builder.appName;
        this.resourceGroup = builder.resourceGroup;
        this.region = builder.region;
        this.pricingTier = builder.pricingTier;
        this.servicePlanName = builder.servicePlanName;
        this.servicePlanResourceGroup = builder.servicePlanResourceGroup;
        this.azure = builder.azure;
        this.settings = builder.settings;
        this.log = builder.log;
    }
}
