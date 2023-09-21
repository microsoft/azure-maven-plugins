/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps;

import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Providers;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppModule;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class AzureContainerApps extends AbstractAzService<AzureContainerAppsServiceSubscription, ContainerAppsApiManager> {

    public AzureContainerApps() {
        super("Microsoft.App");
    }

    @Nonnull
    @Override
    protected AzureContainerAppsServiceSubscription newResource(@Nonnull ContainerAppsApiManager containerAppsApiManager) {
        return new AzureContainerAppsServiceSubscription(containerAppsApiManager.serviceClient().getSubscriptionId(), this);
    }

    public ContainerAppModule containerApps(@Nonnull String subscriptionId) {
        final AzureContainerAppsServiceSubscription rm = get(subscriptionId, null);
        return Objects.requireNonNull(rm).containerApps();
    }

    public ContainerAppsEnvironmentModule environments(@Nonnull String subscriptionId) {
        final AzureContainerAppsServiceSubscription rm = get(subscriptionId, null);
        return Objects.requireNonNull(rm).environments();
    }

    @Nullable
    @Override
    protected ContainerAppsApiManager loadResourceFromAzure(@Nonnull String subscriptionId, @Nullable String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String tenantId = account.getSubscription(subscriptionId).getTenantId();
        final AzureConfiguration config = Azure.az().config();
        final AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, account.getEnvironment());
        // todo: migrate resource provider related codes to common library
        final Providers providers = ResourceManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(config.getLogLevel()))
            .withPolicy(config.getUserAgentPolicy())
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
            .withSubscription(subscriptionId).providers();
        return ContainerAppsApiManager
            .configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(config.getLogLevel()))
            .withPolicy(config.getUserAgentPolicy())
            .withPolicy(new ProviderRegistrationPolicy(providers)) // add policy to auto register resource providers
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container Apps";
    }

    public String getServiceNameForTelemetry() {
        return "containerapps";
    }
}
