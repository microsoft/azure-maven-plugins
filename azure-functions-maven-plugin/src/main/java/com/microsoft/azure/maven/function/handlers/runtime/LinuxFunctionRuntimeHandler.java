/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionRuntimeStack;
import com.microsoft.azure.management.appservice.WebAppBase;

public class LinuxFunctionRuntimeHandler extends FunctionRuntimeHandler {

    public static class Builder extends FunctionRuntimeHandler.Builder<Builder>{

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
    public WebAppBase.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        final AppServicePlan appServicePlan = getAppServicePlan();
        final FunctionApp.DefinitionStages.Blank functionApp = defineFunction();
        FunctionApp.DefinitionStages.WithCreate withCreate;
        FunctionApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage;
        if (appServicePlan == null) {
            FunctionApp.DefinitionStages.NewAppServicePlanWithGroup withRegion = functionApp.withRegion(this.region);
            if (getResourceGroup() == null) {
                withCreate = withRegion.withNewResourceGroup(resourceGroup);
            } else {
                withCreate = withRegion.withExistingResourceGroup(resourceGroup);
            }
            if (pricingTier == null) {
                withDockerContainerImage = withCreate.withNewLinuxConsumptionPlan();
            } else {
                withDockerContainerImage = withCreate.withNewLinuxAppServicePlan(pricingTier);
            }
        } else {
            FunctionApp.DefinitionStages.ExistingLinuxPlanWithGroup withGroup = functionApp.withExistingLinuxAppServicePlan(appServicePlan);
            if (getResourceGroup() == null) {
                withDockerContainerImage = withGroup.withNewResourceGroup(resourceGroup);
            } else {
                withDockerContainerImage = withGroup.withExistingResourceGroup(resourceGroup);
            }
        }
        return withDockerContainerImage.withBuiltInImage(FunctionRuntimeStack.JAVA_8);
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
