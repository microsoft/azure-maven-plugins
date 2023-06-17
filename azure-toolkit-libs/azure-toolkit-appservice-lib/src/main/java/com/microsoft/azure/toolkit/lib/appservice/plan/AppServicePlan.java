/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.plan;

import com.azure.resourcemanager.appservice.fluent.models.AppServicePlanInner;
import com.azure.resourcemanager.appservice.models.ProvisioningState;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class AppServicePlan extends AbstractAzResource<AppServicePlan, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.AppServicePlan>
    implements Deletable {

    protected AppServicePlan(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AppServicePlanModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected AppServicePlan(@Nonnull AppServicePlan origin) {
        super(origin);
    }

    protected AppServicePlan(@Nonnull com.azure.resourcemanager.appservice.models.AppServicePlan remote, @Nonnull AppServicePlanModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull com.azure.resourcemanager.appservice.models.AppServicePlan plan) {
        return Optional.ofNullable(plan.innerModel()).map(AppServicePlanInner::provisioningState).map(ProvisioningState::toString).orElse(Status.UNKNOWN);
    }

    public List<WebApp> getWebApps() {
        return Azure.az(AzureWebApp.class).webApps(this.getSubscriptionId()).list().stream()
            .filter(webapp -> StringUtils.equals(Objects.requireNonNull(webapp.getAppServicePlan()).getId(), this.getId()))
            .collect(Collectors.toList());
    }

    public PricingTier getPricingTier() {
        return this.remoteOptional().map(com.azure.resourcemanager.appservice.models.AppServicePlan::pricingTier)
            .map(t -> PricingTier.fromString(t.toSkuDescription().tier(), t.toSkuDescription().size()))
            .orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return this.remoteOptional().map(Resource::regionName).map(Region::fromName).orElse(null);
    }

    public OperatingSystem getOperatingSystem() {
        return this.remoteOptional().map(r -> r.operatingSystem().name()).map(OperatingSystem::fromString).orElse(null);
    }
}
