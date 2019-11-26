/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebAppBase;

public class WindowsFunctionRuntimeHandler extends FunctionRuntimeHandler {

    public static class Builder extends FunctionRuntimeHandler.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public WindowsFunctionRuntimeHandler build() {
            return new WindowsFunctionRuntimeHandler(this);
        }
    }

    protected WindowsFunctionRuntimeHandler(Builder builder) {
        super(builder);
    }

    @Override
    public WebAppBase.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan appServicePlan = getAppServicePlan();
        final FunctionApp.DefinitionStages.Blank functionApp = defineFunction();
        FunctionApp.DefinitionStages.WithCreate withCreate;
        if (appServicePlan == null) {
            final FunctionApp.DefinitionStages.NewAppServicePlanWithGroup withRegion = functionApp.withRegion(this.region);
            if (getResourceGroup() == null) {
                withCreate = withRegion.withNewResourceGroup(resourceGroup);
            } else {
                withCreate = withRegion.withExistingResourceGroup(resourceGroup);
            }
            if (pricingTier == null) {
                withCreate = withCreate.withNewConsumptionPlan();
            } else {
                withCreate = withCreate.withNewAppServicePlan(pricingTier);
            }
        } else {
            final FunctionApp.DefinitionStages.ExistingAppServicePlanWithGroup withGroup = functionApp.withExistingAppServicePlan(appServicePlan);
            if (getResourceGroup() == null) {
                withCreate = withGroup.withNewResourceGroup(resourceGroup);
            } else {
                withCreate = withGroup.withExistingResourceGroup(resourceGroup);
            }
        }
        return withCreate;
    }

    @Override
    public WebAppBase.Update updateAppRuntime(FunctionApp app) throws Exception {
        return app.update();
    }

    @Override
    public AppServicePlan updateAppServicePlan(FunctionApp app) throws Exception {

        return null;
    }
}
