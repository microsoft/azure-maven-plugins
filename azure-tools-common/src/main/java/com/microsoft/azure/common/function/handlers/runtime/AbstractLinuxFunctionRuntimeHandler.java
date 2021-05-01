/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers.runtime;

import com.microsoft.azure.common.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractLinuxFunctionRuntimeHandler extends FunctionRuntimeHandler {

    private static final FunctionExtensionVersion LINUX_MINIMUM_VERSION = FunctionExtensionVersion.VERSION_3;
    private static final String FUNCTION_EXTENSION_VERSION_NOT_SUPPORTED = "Linux function is not fully supported in current version %s, " +
            "please set `FUNCTION_EXTENSION_VERSION` to `~3` for better experience";

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
                result = StringUtils.isEmpty(servicePlanName) ? withCreate.withNewLinuxConsumptionPlan() :
                        withCreate.withNewLinuxConsumptionPlan(servicePlanName);
            } else {
                result = StringUtils.isEmpty(servicePlanName) ? withCreate.withNewLinuxAppServicePlan(pricingTier) :
                        withCreate.withNewLinuxAppServicePlan(servicePlanName, pricingTier);
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

    protected void checkFunctionExtensionVersion() {
        if (functionExtensionVersion.getValue() < LINUX_MINIMUM_VERSION.getValue()) {
            Log.warn(String.format(FUNCTION_EXTENSION_VERSION_NOT_SUPPORTED, functionExtensionVersion.getVersion()));
        }
    }
}
