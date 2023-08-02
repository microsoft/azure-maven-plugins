/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.containerservice.ContainerServiceManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class AzureContainerService extends AbstractAzService<ContainerServiceSubscription, ContainerServiceManager> {
    public AzureContainerService() {
        super("Microsoft.ContainerService");
    }

    @Nonnull
    @Override
    protected ContainerServiceSubscription newResource(@Nonnull ContainerServiceManager containerServiceManager) {
        return new ContainerServiceSubscription(containerServiceManager.subscriptionId(), this);
    }

    @Nonnull
    public KubernetesClusterModule kubernetes(@Nonnull String subscriptionId) {
        final ContainerServiceSubscription serviceSubscription = get(subscriptionId, null);
        assert serviceSubscription != null;
        return serviceSubscription.kubernetes();
    }

    @Nullable
    @Override
    protected ContainerServiceManager loadResourceFromAzure(@Nonnull String subscriptionId, @Nullable String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return ContainerServiceManager.configure()
                .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
                .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
                .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Kubernetes services";
    }

    public String getServiceNameForTelemetry() {
        return "kubernetes";
    }
}
