/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Providers;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.AzureService.getUserAgentPolicy;

public class AzureApplicationInsights extends AbstractAzService<ApplicationInsightsResourceManager, ApplicationInsightsManager> {
    public AzureApplicationInsights() {
        super("Microsoft.Insights");
    }

    @Nonnull
    @Override
    protected ApplicationInsightsResourceManager newResource(@Nonnull ApplicationInsightsManager applicationInsightsManager) {
        return new ApplicationInsightsResourceManager(applicationInsightsManager.serviceClient().getSubscriptionId(), this);
    }

    @Nonnull
    public ApplicationInsightsModule applicationInsights(@Nonnull String subscriptionId) {
        final ApplicationInsightsResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.applicationInsights();
    }

    @Nullable
    @Override
    protected ApplicationInsightsManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String tenantId = account.getSubscription(subscriptionId).getTenantId();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogOptions logOptions = new HttpLogOptions();
        logOptions.setLogLevel(Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE));
        final AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, account.getEnvironment());
        // todo: migrate resource provider related codes to common library
        final Providers providers = ResourceManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
                .withPolicy(getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId).providers();
        return ApplicationInsightsManager
                .configure()
                .withHttpClient(AzureService.getDefaultHttpClient())
                .withLogOptions(logOptions)
                .withPolicy(getUserAgentPolicy(userAgent))
                .withPolicy(new ProviderRegistrationPolicy(providers)) // add policy to auto register resource providers
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Application Insights";
    }
}
