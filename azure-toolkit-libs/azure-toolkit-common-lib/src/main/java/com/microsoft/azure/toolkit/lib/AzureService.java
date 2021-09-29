/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
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
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.NoopAddressResolverGroup;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public interface AzureService<T extends IAzureBaseResource> extends IAzureModule<T, IAzureBaseResource> {
    default List<Subscription> getSubscriptions() {
        return Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
    }

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
            .ifPresent(list -> result.addAll(list.stream().map(Region::fromName).filter(allRegionList::contains).collect(Collectors.toList())));
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
            .withHttpClient(getDefaultHttpClient())
            .withPolicy(getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
            .withSubscription(subscriptionId).providers();
        return ResourceManager.configure()
            .withHttpClient(getDefaultHttpClient())
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

    class HttpClientHolder {
        private static HttpClient defaultHttpClient = null;

        private static synchronized HttpClient createHttpClient() {
            if (defaultHttpClient != null) {
                return defaultHttpClient;
            }

            AddressResolverGroup resolverGroup;
            ProxyOptions proxyOptions = null;
            final AzureConfiguration config = Azure.az().config();
            if (StringUtils.isNotBlank(config.getProxySource())) {
                proxyOptions = new ProxyOptions(ProxyOptions.Type.HTTP,
                    new InetSocketAddress(config.getHttpProxyHost(), config.getHttpProxyPort())
                );
                if (StringUtils.isNoneBlank(config.getProxyUsername(), config.getProxyPassword())) {
                    proxyOptions.setCredentials(config.getProxyUsername(), config.getProxyPassword());
                }
                resolverGroup = NoopAddressResolverGroup.INSTANCE;
            } else {
                resolverGroup = DefaultAddressResolverGroup.INSTANCE;
            }
            reactor.netty.http.client.HttpClient nettyHttpClient =
                reactor.netty.http.client.HttpClient.create()
                    .resolver(resolverGroup);
            NettyAsyncHttpClientBuilder builder = new NettyAsyncHttpClientBuilder(nettyHttpClient);
            Optional.ofNullable(proxyOptions).map(proxy -> builder.proxy(proxy));
            defaultHttpClient = builder.build();
            return defaultHttpClient;
        }
    }

    static HttpClient getDefaultHttpClient() {
        return HttpClientHolder.createHttpClient();
    }
}
