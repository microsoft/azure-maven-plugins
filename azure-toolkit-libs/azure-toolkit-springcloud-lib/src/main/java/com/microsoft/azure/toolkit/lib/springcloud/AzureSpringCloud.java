/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureSpringCloud extends SubscriptionScoped<AzureSpringCloud> implements AzureService {
    public AzureSpringCloud() { // for SPI
        super(AzureSpringCloud::new);
    }

    private AzureSpringCloud(@Nonnull final List<Subscription> subscriptions) { // for creating scoped AzureSpringCloud
        super(AzureSpringCloud::new, subscriptions);
    }

    @Nullable
    @Cacheable(cacheName = "asc/cluster/{}", key = "$name")
    public SpringCloudCluster cluster(@Nonnull String name) {
        return this.clusters().stream()
                .filter((s) -> Objects.equals(s.name(), name))
                .findAny().orElse(null);
    }

    @Nonnull
    SpringCloudCluster cluster(@Nonnull SpringService remote) {
        return this.cluster(new SpringCloudClusterEntity(remote));
    }

    @Nonnull
    private SpringCloudCluster cluster(@Nonnull SpringCloudClusterEntity cluster) {
        return new SpringCloudCluster(cluster, this.getClient(cluster.getSubscriptionId()));
    }

    @Nonnull
    @Preload
    public List<SpringCloudCluster> clusters(boolean... force) {
        return this.getSubscriptions().stream().parallel()
                .flatMap(s -> clusters(s.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @Nonnull
    @Cacheable(cacheName = "asc/{}/clusters", key = "$subscriptionId", condition = "!(force&&force[0])")
    private List<SpringCloudCluster> clusters(@Nonnull String subscriptionId, boolean... force) {
        return getClient(subscriptionId).list().stream()
                .map(this::cluster)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "asc/{}/client", key = "$subscriptionId")
    protected SpringServices getClient(final String subscriptionId) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return AppPlatformManager.configure()
                .withLogLevel(logLevel)
                .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .springServices();
    }

    private HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
