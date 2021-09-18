/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.validator.SchemaValidator;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@AllArgsConstructor
public class CreateOrUpdateAppServicePlanTask extends AzureTask<IAppServicePlan> {
    private static final String CREATE_APP_SERVICE_PLAN = "Creating app service plan %s...";
    private static final String CREATE_APP_SERVICE_PLAN_DONE = "Successfully created app service plan %s.";
    private static final String CREATE_NEW_APP_SERVICE_PLAN = "createNewAppServicePlan";
    @Nonnull
    private AppServicePlanConfig config;
    @Nullable
    private AppServicePlanConfig defaultConfig;

    @AzureOperation(name = "appservice|plan.create_update", params = {"this.config.servicePlanName()"}, type = AzureOperation.Type.SERVICE)
    public IAppServicePlan execute() {
        SchemaValidator.getInstance().validateAndThrow("appservice/AppServicePlan", config);
        final AzureAppService az = Azure.az(AzureAppService.class).subscription(config.subscriptionId());
        final IAppServicePlan appServicePlan = az.appServicePlan(config.servicePlanResourceGroup(), config.servicePlanName());
        final String servicePlanName = config.servicePlanName();
        if (!appServicePlan.exists()) {
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_PLAN, servicePlanName));
            AzureTelemetry.getActionContext().setProperty(CREATE_NEW_APP_SERVICE_PLAN, String.valueOf(true));
            if (this.defaultConfig == null) {
                throw new AzureToolkitRuntimeException("Cannot create service without default config.");
            }
            final Region regionOrDefault = ObjectUtils.firstNonNull(this.config.region(), this.defaultConfig.region());
            new CreateResourceGroupTask(this.config.subscriptionId(), config.servicePlanResourceGroup(), regionOrDefault).execute();
            appServicePlan.create()
                .withName(servicePlanName)
                .withResourceGroup(config.servicePlanResourceGroup())
                .withPricingTier(ObjectUtils.firstNonNull(config.pricingTier(), defaultConfig.pricingTier()))
                .withRegion(regionOrDefault)
                .withOperatingSystem(ObjectUtils.firstNonNull(config.os(), defaultConfig.os()))
                .commit();
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_PLAN_DONE, appServicePlan.name()));
        } else {
            if (config.region() != null && !Objects.equals(config.region(), Region.fromName(appServicePlan.entity().getRegion()))) {
                AzureMessager.getMessager().warning(String.format("Skip region update for existing service plan '%s' since it is not allowed.",
                    appServicePlan.name()));
            }
            if (config.pricingTier() != null) {
                // apply pricing tier to service plan
                appServicePlan.update().withPricingTier(config.pricingTier()).commit();
            }
        }

        return appServicePlan;
    }
}
