/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanModule;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAppService extends AbstractAzService<AppServiceServiceSubscription, AppServiceManager> {
    public AzureAppService() {
        super("Microsoft.Web"); // for SPI
    }

    @Nonnull
    public AppServicePlanModule plans(@Nonnull String subscriptionId) {
        final AppServiceServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getPlanModule();
    }

    @Nonnull
    public List<AppServicePlan> plans() {
        return this.list().stream().flatMap(m -> m.plans().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    public AppServicePlan plan(String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return this.plans(id.subscriptionId()).get(id.name(), id.resourceGroupName());
    }

    @Nonnull
    @Override
    protected AppServiceManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return AppServiceManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogLevel(logLevel)
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent)) // set user agent with policy
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected AppServiceServiceSubscription newResource(@Nonnull AppServiceManager remote) {
        return new AppServiceServiceSubscription(remote, this);
    }

    @Nullable
    @Override
    public <E> E getById(@Nonnull String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        if (resourceId.resourceType().equals(AppServicePlanModule.NAME)) {
            return super.doGetById(id);
        } else {
            boolean isFunctionRelated = false;
            try {
                isFunctionRelated = Optional.ofNullable(this.get(resourceId.subscriptionId(), null))
                    .map(AbstractAzResource::getRemote).map(r -> r.resourceManager().genericResources().getById(id))
                    .filter(r -> StringUtils.containsIgnoreCase(r.kind(), "function")).isPresent();
            } catch (ManagementException e) {
                if (e.getResponse().getStatusCode() != 404) { // Java SDK throw exception with 200 response, swallow exception in this case
                    throw e;
                }
            }
            if (isFunctionRelated) {
                return Azure.az(AzureFunctions.class).doGetById(id);
            } else {
                return Azure.az(AzureWebApp.class).doGetById(id);
            }
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "App Services";
    }
}
