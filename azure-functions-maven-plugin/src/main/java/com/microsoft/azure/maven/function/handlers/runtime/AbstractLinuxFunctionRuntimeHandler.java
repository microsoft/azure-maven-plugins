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
        final FunctionApp.DefinitionStages.Blank functionApp = defineFunction();
        final FunctionApp.DefinitionStages.WithCreate withCreate;
        final FunctionApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage;
        if (appServicePlan == null) {
            final FunctionApp.DefinitionStages.NewAppServicePlanWithGroup appWithNewServicePlan = functionApp.withRegion(this.region);
            if (getResourceGroup() == null) {
                withCreate = appWithNewServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                withCreate = appWithNewServicePlan.withExistingResourceGroup(resourceGroup);
            }
            if (pricingTier == null) {
                withDockerContainerImage = withCreate.withNewLinuxConsumptionPlan();
            } else {
                withDockerContainerImage = withCreate.withNewLinuxAppServicePlan(pricingTier);
            }
        } else {
            final FunctionApp.DefinitionStages.ExistingLinuxPlanWithGroup appWithExistingServicePlan =
                    functionApp.withExistingLinuxAppServicePlan(appServicePlan);
            if (getResourceGroup() == null) {
                withDockerContainerImage = appWithExistingServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                withDockerContainerImage = appWithExistingServicePlan.withExistingResourceGroup(resourceGroup);
            }
        }
        return withDockerContainerImage;
    }
}
