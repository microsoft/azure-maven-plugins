/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.ProviderResourceType;
import com.azure.resourcemanager.resources.models.Providers;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureModule;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public interface AzureService<T extends IAzureBaseResource> extends IAzureModule<T, IAzureBaseResource> {
    default List<Subscription> getSubscriptions() {
        return Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
    }

    @SuppressWarnings("checkstyle:Indentation")
    default List<Region> listSupportedRegions(String subscriptionId) {
        String[] names = StringUtils.split(name(), "/");
        if (names.length != 2) {
            throw new AzureToolkitRuntimeException(String.format("It is illegal to list regions for service '%s'.", name()));
        }
        final String provider = names[0];
        final String resourceType = names[1];
        List<Region> allRegionList = az(IAzureAccount.class).listRegions(subscriptionId);
        List<Region> result = new ArrayList<>();
        final ResourceManager resourceManager = getResourceManager(subscriptionId);
        resourceManager.providers().getByName(provider).resourceTypes()
            .stream().filter(type -> StringUtils.equalsIgnoreCase(type.resourceType(), resourceType))
            .findAny().map(ProviderResourceType::locations)
            .ifPresent(list -> {
                final List<Region> regionListByResource = list.stream().map(Region::fromName).collect(Collectors.toList());
                result.addAll(CollectionUtils.intersection(regionListByResource, allRegionList));
            });
        return result.isEmpty() ? allRegionList : result;
    }

    @Cacheable(cacheName = "resource/{}/manager", key = "$subscriptionId")
    default ResourceManager getResourceManager(String subscriptionId) {
        // make sure it is signed in.
        final IAccount account = az(IAzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logDetailLevel = config.getLogLevel() == null ?
            HttpLogDetailLevel.NONE : HttpLogDetailLevel.valueOf(config.getLogLevel());
        final AzureProfile azureProfile = new AzureProfile(account.getEnvironment());

        final Providers providers = ResourceManager.configure()
            .withPolicy(getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
            .withSubscription(subscriptionId).providers();
        return ResourceManager.configure()
            .withLogLevel(logDetailLevel)
            .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
            .withPolicy(new ProviderRegistrationPolicy(providers)) // add policy to auto register resource providers
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
            .withSubscription(subscriptionId);
    }

    static HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
