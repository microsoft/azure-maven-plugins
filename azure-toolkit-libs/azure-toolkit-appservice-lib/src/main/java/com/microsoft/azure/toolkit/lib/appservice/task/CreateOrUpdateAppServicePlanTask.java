/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class CreateOrUpdateAppServicePlanTask extends AzureTask<IAppServicePlan> {
    private static final String CREATE_APP_SERVICE_PLAN = "Creating app service plan %s...";
    private static final String CREATE_APP_SERVICE_PLAN_DONE = "Successfully created app service plan %s.";
    private static final String CREATE_NEW_APP_SERVICE_PLAN = "createNewAppServicePlan";
    private AppServicePlanConfig config;

    @AzureOperation(name = "appservice|plan.create_update", params = {"this.config.servicePlanName()"}, type = AzureOperation.Type.SERVICE)
    public IAppServicePlan execute() {
        final AzureAppService az = Azure.az(AzureAppService.class).subscription(config.subscriptionId());
        az.appServicePlan(config.servicePlanResourceGroup(), config.servicePlanName());
        final IAppServicePlan appServicePlan = az.appServicePlan(config.servicePlanResourceGroup(), config.servicePlanName());
        final String servicePlanName = config.servicePlanName();
        if (!appServicePlan.exists()) {
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_PLAN, servicePlanName));
            AzureTelemetry.getActionContext().setProperty(CREATE_NEW_APP_SERVICE_PLAN, String.valueOf(true));
            appServicePlan.create()
                .withName(servicePlanName)
                .withResourceGroup(config.servicePlanResourceGroup())
                .withPricingTier(config.pricingTier())
                .withRegion(config.region())
                .withOperatingSystem(config.os())
                .commit();
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_PLAN_DONE, appServicePlan.name()));
        } else if (config.pricingTier() != null) {
            if (config.region() != null && !Objects.equals(config.region(), Region.fromName(appServicePlan.entity().getRegion()))) {
                AzureMessager.getMessager().warning(String.format("Cannot update region for existing service plan '%s'.", appServicePlan.name()));
            }
            appServicePlan.update().withPricingTier(config.pricingTier()).commit();
        }
        return appServicePlan;
    }
}
