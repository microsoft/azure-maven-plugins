/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.ResourceNameAvailabilityInner;
import com.azure.resourcemanager.appservice.models.CheckNameResourceTypes;
import com.azure.resourcemanager.appservice.models.ResourceNameAvailabilityRequest;
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppModule;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanModule;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class AppServiceServiceSubscription extends AbstractAzServiceSubscription<AppServiceServiceSubscription, AppServiceManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final FunctionAppModule functionAppModule;
    @Nonnull
    private final WebAppModule webAppModule;
    @Nonnull
    private final AppServicePlanModule planModule;

    protected AppServiceServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureAppService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.functionAppModule = new FunctionAppModule(this);
        this.webAppModule = new WebAppModule(this);
        this.planModule = new AppServicePlanModule(this);
    }

    protected AppServiceServiceSubscription(@Nonnull AppServiceManager remote, @Nonnull AzureAppService service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        // it will cause unnecessary preloading/refreshing if webAppModule and functionAppModule
        // are not removed from here, as webAppModule and functionAppModule has already added as
        // sub modules of AzureWebApp and AzureFunction respectively.

        // TODO: remove AzureWebApp and AzureFunction
        return Collections.singletonList(planModule);
    }

    @Nonnull
    public FunctionAppModule functionApps() {
        return this.functionAppModule;
    }

    @Nonnull
    public AppServicePlanModule plans() {
        return this.planModule;
    }

    @Nonnull
    public WebAppModule webApps() {
        return this.webAppModule;
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.webAppModule.getName());
    }

    @AzureOperation(name = "appservice.check_name.name", params = "name", type = AzureOperation.Type.REQUEST)
    public Availability checkNameAvailability(String name) {
        final ResourceNameAvailabilityInner result = Objects.requireNonNull(this.getRemote()).webApps().manager()
            .serviceClient().getResourceProviders().checkNameAvailability(new ResourceNameAvailabilityRequest()
                .withName(name).withType(CheckNameResourceTypes.MICROSOFT_WEB_SITES));
        return new Availability(result.nameAvailable(), result.reason().toString(), result.message());
    }
}

