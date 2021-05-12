/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;

import java.util.Optional;

public class AzureResourceManagerFactory {

    @Cacheable(cacheName = "AzureResourceManager", key = "$subscriptionId")
    public static AzureResourceManager produce(String subscriptionId) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(account.getEnvironment());
        /**
         * TODO: refactor to reuse AzureResourceManager instance for same subscriptionId, rather than re-new one.
         */
        return AzureResourceManager.configure()
                .withLogLevel(logLevel)
                .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId);
    }

    private static HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
