/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;

public abstract class AbstractLinuxFunctionRuntimeHandler extends FunctionRuntimeHandler {

    protected AbstractLinuxFunctionRuntimeHandler(Builder builder) {
        super(builder);
    }

    protected FunctionApp.DefinitionStages.WithDockerContainerImage defineLinuxFunction() {
        final AppServicePlan appServicePlan = getAppServicePlan();
        final FunctionApp.DefinitionStages.Blank blankFunctionApp = defineFunction();
        final FunctionApp.DefinitionStages.WithDockerContainerImage result;
        if (appServicePlan == null) {
            final FunctionApp.DefinitionStages.NewAppServicePlanWithGroup appWithNewServicePlan = blankFunctionApp.withRegion(this.region);
            final FunctionApp.DefinitionStages.WithCreate withCreate;
            if (getResourceGroup() == null) {
                withCreate = appWithNewServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                withCreate = appWithNewServicePlan.withExistingResourceGroup(resourceGroup);
            }
            if (pricingTier == null) {
                result = withCreate.withNewLinuxConsumptionPlan();
            } else {
                result = withCreate.withNewLinuxAppServicePlan(pricingTier);
            }
        } else {
            final FunctionApp.DefinitionStages.ExistingLinuxPlanWithGroup appWithExistingServicePlan =
                    blankFunctionApp.withExistingLinuxAppServicePlan(appServicePlan);
            if (getResourceGroup() == null) {
                result = appWithExistingServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                result = appWithExistingServicePlan.withExistingResourceGroup(resourceGroup);
            }
        }
        return result;
    }
}
