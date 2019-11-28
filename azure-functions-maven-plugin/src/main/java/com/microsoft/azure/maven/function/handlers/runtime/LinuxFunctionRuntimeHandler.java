/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionRuntimeStack;

public class LinuxFunctionRuntimeHandler extends FunctionRuntimeHandler {

    public static class Builder extends FunctionRuntimeHandler.Builder<Builder> {

        @Override
        public FunctionRuntimeHandler build() {
            return new LinuxFunctionRuntimeHandler(self());
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    protected LinuxFunctionRuntimeHandler(Builder builder) {
        super(builder);
    }

    @Override
    public FunctionApp.DefinitionStages.WithCreate defineAppWithRuntime() {
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
        return withDockerContainerImage.withBuiltInImage(FunctionRuntimeStack.JAVA_8);
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) {
        return app.update();
    }

    @Override
    public AppServicePlan updateAppServicePlan(FunctionApp app) {
        // Todo: update app service plan
        return null;
    }
}
