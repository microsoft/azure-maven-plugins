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
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureGroup extends SubscriptionScoped<AzureGroup> implements AzureService {

    public AzureGroup() { // for SPI
        super(AzureGroup::new);
    }

    private AzureGroup(@Nonnull final List<Subscription> subscriptions) {
        super(AzureGroup::new, subscriptions);
    }

    @Preload
    public List<ResourceGroup> list(boolean... force) {
        return getSubscriptions().stream().parallel()
                .flatMap(s -> list(s.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "resource/{}/groups", key = "$sid", condition = "!(force&&force[0])")
    public List<ResourceGroup> list(String sid, boolean... force) {
        return getResourceManager(sid).resourceGroups().listAsync()
                .map(AzureGroup::fromResource)
                .collect(Collectors.toList()).block();
    }

    public ResourceGroup getById(@Nonnull String idStr) {
        final ResourceId id = ResourceId.fromString(idStr);
        return get(id.subscriptionId(), id.resourceGroupName());
    }

    public ResourceGroup getByName(@Nonnull String name) {
        return get(getDefaultSubscription().getId(), name);
    }

    @Cacheable(cacheName = "resource/{}/group/{}", key = "$sid/$name")
    public ResourceGroup get(@Nonnull String sid, @Nonnull String name) {
        return fromResource(getResourceManager(sid).resourceGroups().getByName(name));
    }

    public ResourceGroup create(String name, String region) {
        if (StringUtils.isNoneBlank(name, region)) {
            final com.azure.resourcemanager.resources.models.ResourceGroup result = getResourceManager(getDefaultSubscription().getId())
                    .resourceGroups().define(name)
                    .withRegion(region).create();
            return fromResource(result);
        }
        throw new AzureToolkitRuntimeException("Please provide both name and region to create a resource group.");
    }

    public void delete(String name) {
        delete(getDefaultSubscription().getId(), name);
    }

    public void delete(String subscriptionId, String name) {
        getResourceManager(subscriptionId)
                .resourceGroups().deleteByName(name);
    }

    public boolean checkNameAvailability(String subscriptionId, String name) {
        return !getResourceManager(subscriptionId).resourceGroups().contain(name);
    }

    private static ResourceGroup fromResource(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup resource) {
        final ResourceId resourceId = ResourceId.fromString(resource.id());
        String subscriptionId = resourceId.subscriptionId();
        String name = resource.name();
        String region = resource.regionName();
        String id = resource.id();
        return ResourceGroup.builder().subscriptionId(subscriptionId).id(id).name(name).region(region).build();
    }

    @Cacheable(cacheName = "resource/{}/manager", key = "$subscriptionId")
    private ResourceManager getResourceManager(String subscriptionId) {
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
