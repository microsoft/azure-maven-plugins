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
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public class AppServiceResourceManager extends AbstractAzResourceManager<AppServiceResourceManager, AppServiceManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final FunctionAppModule functionAppModule;
    @Nonnull
    private final WebAppModule webAppModule;
    @Nonnull
    private final AppServicePlanModule planModule;

    protected AppServiceResourceManager(@Nonnull String subscriptionId, @Nonnull AzureAppService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.functionAppModule = new FunctionAppModule(this);
        this.webAppModule = new WebAppModule(this);
        this.planModule = new AppServicePlanModule(this);
    }

    protected AppServiceResourceManager(@Nonnull AppServiceManager remote, @Nonnull AzureAppService service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, AppServiceResourceManager, ?>> getSubModules() {
        return Arrays.asList(webAppModule, functionAppModule, planModule);
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

    @AzureOperation(name = "appservice.check_name.app", params = "name", type = AzureOperation.Type.SERVICE)
    public CheckNameAvailabilityResultEntity checkNameAvailability(String name) {
        final ResourceNameAvailabilityInner result = Objects.requireNonNull(this.getRemote()).webApps().manager()
            .serviceClient().getResourceProviders().checkNameAvailability(new ResourceNameAvailabilityRequest()
                .withName(name).withType(CheckNameResourceTypes.MICROSOFT_WEB_SITES));
        return new CheckNameAvailabilityResultEntity(result.nameAvailable(), result.reason().toString(), result.message());
    }
}
