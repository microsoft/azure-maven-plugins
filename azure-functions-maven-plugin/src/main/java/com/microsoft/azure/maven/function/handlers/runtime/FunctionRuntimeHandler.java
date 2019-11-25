/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.maven.handlers.runtime.BaseRuntimeHandler;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import org.codehaus.plexus.util.StringUtils;

public abstract class FunctionRuntimeHandler extends BaseRuntimeHandler<FunctionApp> {

    RuntimeConfiguration runtimeConfiguration;

    public abstract static class Builder<T extends FunctionRuntimeHandler.Builder<T>> extends BaseRuntimeHandler.Builder<T> {

        RuntimeConfiguration runtimeConfiguration;

        public T runtime(final RuntimeConfiguration value) {
            this.runtimeConfiguration = value;
            return self();
        }

        public abstract FunctionRuntimeHandler build();

        protected abstract T self();
    }

    protected FunctionRuntimeHandler(Builder<?> builder) {
        super(builder);
        this.runtimeConfiguration = builder.runtimeConfiguration;
    }

    protected FunctionApp.DefinitionStages.Blank defineFunction() {
        return azure.appServices().functionApps().define(appName);
    }

    protected AppServicePlan getAppServicePlan() {
        return AppServiceUtils.getAppServicePlan(servicePlanName, azure, resourceGroup, servicePlanResourceGroup);
    }

    protected String changeAppServicePlan() {
        return StringUtils.isEmpty(servicePlanResourceGroup) ? resourceGroup : servicePlanResourceGroup;
    }


    protected ResourceGroup getResourceGroup() {
        return azure.resourceGroups().getByName(resourceGroup);
    }
}
