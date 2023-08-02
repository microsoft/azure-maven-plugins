/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.containerregistry.ContainerRegistryManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Providers;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class AzureContainerRegistry extends AbstractAzService<AzureContainerRegistryServiceSubscription, ContainerRegistryManager> {

    public AzureContainerRegistry() {
        super("Microsoft.ContainerRegistry");
    }

    @Nonnull
    @Override
    protected AzureContainerRegistryServiceSubscription newResource(@Nonnull ContainerRegistryManager containerRegistryManager) {
        return new AzureContainerRegistryServiceSubscription(containerRegistryManager, this);
    }

    public AzureContainerRegistryModule registry(@Nonnull String subscriptionId) {
        final AzureContainerRegistryServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.registry();
    }

    @Nullable
    @Override
    protected ContainerRegistryManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String tenantId = account.getSubscription(subscriptionId).getTenantId();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogOptions logOptions = new HttpLogOptions();
        logOptions.setLogLevel(Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE));
        final AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, account.getEnvironment());
        // todo: migrate resource provider related codes to common library
        final Providers providers = ResourceManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId).providers();
        return ContainerRegistryManager
            .configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogOptions(logOptions)
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .withPolicy(new ProviderRegistrationPolicy(providers)) // add policy to auto register resource providers
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container Registries";
    }

    public String getServiceNameForTelemetry() {
        return "acr";
    }
}
