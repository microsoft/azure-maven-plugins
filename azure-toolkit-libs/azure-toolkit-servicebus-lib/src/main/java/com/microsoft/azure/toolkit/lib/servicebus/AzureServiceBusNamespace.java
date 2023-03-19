/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Providers;
import com.azure.resourcemanager.servicebus.ServiceBusManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class AzureServiceBusNamespace extends AbstractAzService<ServiceBusNamespaceSubscription, ServiceBusManager> {
    public AzureServiceBusNamespace() {
        super("Microsoft.ServiceBus");
    }

    @Nonnull
    @Override
    protected ServiceBusNamespaceSubscription newResource(@Nonnull ServiceBusManager serviceBusManager) {
        return new ServiceBusNamespaceSubscription(serviceBusManager.serviceClient().getSubscriptionId(), this);
    }

    @Nullable
    @Override
    protected ServiceBusManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String tenantId = account.getSubscription(subscriptionId).getTenantId();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogOptions logOptions = new HttpLogOptions();
        logOptions.setLogLevel(Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE));
        final AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, account.getEnvironment());
        final Providers providers = ResourceManager.configure()
                .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
                .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId).providers();
        return ServiceBusManager
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
        return "Service Bus Namespace";
    }
}
