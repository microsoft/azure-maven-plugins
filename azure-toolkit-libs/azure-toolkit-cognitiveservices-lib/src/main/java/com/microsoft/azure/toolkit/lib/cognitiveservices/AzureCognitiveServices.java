/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Providers;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AzureCognitiveServices extends AbstractAzService<CognitiveServicesSubscription, CognitiveServicesManager> {

    public AzureCognitiveServices() {
        super("Microsoft.CognitiveServices");
    }

    @Nonnull
    @Override
    protected CognitiveServicesSubscription newResource(@Nonnull CognitiveServicesManager cognitiveServicesManager) {
        return new CognitiveServicesSubscription(cognitiveServicesManager, this);
    }

    @Nonnull
    public CognitiveAccountModule accounts(@Nonnull String subscriptionId) {
        final CognitiveServicesSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.accounts();
    }

    @Cacheable(cacheName = "openai/subscriptions/{}", key = "$subscriptionId")
    public boolean isOpenAIEnabled(@Nonnull String subscriptionId) {
        final CognitiveAccountModule module = accounts(subscriptionId);
        return CollectionUtils.isNotEmpty(module.listSkus(null));
    }

    @Nullable
    @Override
    protected CognitiveServicesManager loadResourceFromAzure(@Nonnull String subscriptionId, @Nullable String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String tenantId = account.getSubscription(subscriptionId).getTenantId();
        final AzureConfiguration config = Azure.az().config();
        final HttpLogOptions logOptions = new HttpLogOptions();
        logOptions.setLogLevel(config.getLogLevel());
        final AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, account.getEnvironment());
        // todo: migrate resource provider related codes to common library
        final Providers providers = ResourceManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withPolicy(config.getUserAgentPolicy())
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
            .withSubscription(subscriptionId).providers();
        return CognitiveServicesManager
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
        return "Azure OpenAI";
    }

    @Override
    public String getServiceNameForTelemetry() {
        return "openai";
    }
}
