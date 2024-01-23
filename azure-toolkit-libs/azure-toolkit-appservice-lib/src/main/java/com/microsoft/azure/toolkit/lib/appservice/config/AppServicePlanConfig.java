/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AppServicePlanConfig {
    private String subscriptionId;

    private String resourceGroupName;

    private String name;

    private OperatingSystem os;

    private Region region;

    private PricingTier pricingTier;

    @Transient
    public static AppServicePlan getAppServicePlan(@Nonnull final AppServicePlanConfig config) {
        final AzureAppService az = Azure.az(AzureAppService.class);
        final AppServicePlanDraft draft = az.plans(config.getSubscriptionId())
            .updateOrCreate(config.getName(), config.getResourceGroupName());
        draft.setOperatingSystem(config.getOs());
        draft.setRegion(config.getRegion());
        draft.setPricingTier(config.getPricingTier());
        return draft;
    }

    @Contract("null->null")
    public static AppServicePlanConfig fromResource(@Nullable AppServicePlan plan) {
        if (Objects.isNull(plan)) {
            return null;
        }
        return AppServicePlanConfig.builder()
            .subscriptionId(plan.getSubscriptionId())
            .resourceGroupName(plan.getResourceGroupName())
            .name(plan.getName())
            .os(plan.getOperatingSystem())
            .region(plan.getRegion())
            .pricingTier(plan.getPricingTier())
            .build();
    }

    @Nonnull
    public AppServicePlan toResource() {
        final AppServicePlan plan = Azure.az(AzureAppService.class).plans(this.subscriptionId).getOrDraft(this.name, this.resourceGroupName);
        if (plan.isDraftForCreating()) {
            final AppServicePlanDraft draft = (AppServicePlanDraft) plan;
            draft.setOperatingSystem(this.os);
            draft.setRegion(this.region);
            draft.setPricingTier(this.pricingTier);
        }
        return plan;
    }
}
