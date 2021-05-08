/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;

public class AzureGroup extends SubscriptionScoped<AzureGroup> implements AzureService {

    public AzureGroup() { // for SPI
        super(AzureGroup::new);
    }

    private AzureGroup(@Nonnull final List<Subscription> subscriptions) {
        super(AzureGroup::new, subscriptions);
    }

    public List<ResourceGroupEntity> list() {
        return Flux.fromIterable(getSubscriptions()).parallel()
                .map(subscription -> getResourceManager(subscription.getId()))
                .flatMap(azureResourceManager -> azureResourceManager.resourceGroups().listAsync())
                .map(ResourceGroupEntity::fromResource)
                .sequential().collectList().block();
    }

    public ResourceGroupEntity getById(@Nonnull String idStr) {
        final ResourceId id = ResourceId.fromString(idStr);
        return get(id.subscriptionId(), id.resourceGroupName());
    }

    public ResourceGroupEntity getByName(@Nonnull String name) {
        return get(getDefaultSubscription().getId(), name);
    }

    public ResourceGroupEntity get(@Nonnull String subscriptionId, @Nonnull String name) {
        return ResourceGroupEntity.fromResource(getResourceManager(subscriptionId).resourceGroups().getByName(name));
    }

    public ResourceGroupEntity create(String name, String region) {
        if (StringUtils.isNoneBlank(name, region)) {
            final com.azure.resourcemanager.resources.models.ResourceGroup result = getResourceManager(getDefaultSubscription().getId())
                    .resourceGroups().define(name)
                    .withRegion(region).create();
            return ResourceGroupEntity.fromResource(result);
        }
        throw new AzureToolkitRuntimeException("Please provide both name and region to create a resource group.");
    }

    public void delete(String name) {
        getResourceManager(getDefaultSubscription().getId())
                .resourceGroups().deleteByName(name);
    }

    @Cacheable(cacheName = "ResourceManager", key = "$subscriptionId")
    public ResourceManager getResourceManager(String subscriptionId) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logDetailLevel = config.getLogLevel() == null ?
                HttpLogDetailLevel.NONE : HttpLogDetailLevel.valueOf(config.getLogLevel());
        final AzureProfile azureProfile = new AzureProfile(account.getEnvironment());
        return ResourceManager.configure()
                .withLogLevel(logDetailLevel)
                .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId);
    }

    private HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
