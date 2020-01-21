/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import org.apache.commons.lang3.StringUtils;

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
    public FunctionApp.DefinitionStages.WithCreate defineAppWithRuntime() {
        final AppServicePlan appServicePlan = getAppServicePlan();
        final FunctionApp.DefinitionStages.Blank functionApp = defineFunction();
        FunctionApp.DefinitionStages.WithCreate appWithCreate;
        if (appServicePlan == null) {
            final FunctionApp.DefinitionStages.NewAppServicePlanWithGroup appWithNewServicePlan =
                    functionApp.withRegion(this.region);
            if (getResourceGroup() == null) {
                appWithCreate = appWithNewServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                appWithCreate = appWithNewServicePlan.withExistingResourceGroup(resourceGroup);
            }
            if (pricingTier == null) {
                appWithCreate = StringUtils.isEmpty(servicePlanName) ? appWithCreate.withNewConsumptionPlan() :
                        appWithCreate.withNewConsumptionPlan(servicePlanName);
            } else {
                appWithCreate = StringUtils.isEmpty(servicePlanName) ? appWithCreate.withNewAppServicePlan(pricingTier) :
                        appWithCreate.withNewAppServicePlan(servicePlanName, pricingTier);
            }
        } else {
            final FunctionApp.DefinitionStages.ExistingAppServicePlanWithGroup appWithExistingServicePlan =
                    functionApp.withExistingAppServicePlan(appServicePlan);
            if (getResourceGroup() == null) {
                appWithCreate = appWithExistingServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                appWithCreate = appWithExistingServicePlan.withExistingResourceGroup(resourceGroup);
            }
        }
        return appWithCreate;
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) {
        return app.update();
    }
}
