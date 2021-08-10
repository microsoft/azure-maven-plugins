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
import com.azure.core.management.exception.ManagementException;
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
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AzureSpringCloud extends SubscriptionScoped<AzureSpringCloud>
        implements AzureService, AzureOperationEvent.Source<AzureSpringCloud> {
    public AzureSpringCloud() { // for SPI
        super(AzureSpringCloud::new);
    }

    private AzureSpringCloud(@Nonnull final List<Subscription> subscriptions) { // for creating scoped AzureSpringCloud
        super(AzureSpringCloud::new, subscriptions);
    }

    @Nullable
    @Cacheable(cacheName = "asc/cluster/{}", key = "$name")
    @AzureOperation(name = "springcloud|cluster.get.name", params = {"name"}, type = AzureOperation.Type.SERVICE)
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
    @AzureOperation(name = "springcloud|cluster.list.subscription|selected", type = AzureOperation.Type.SERVICE)
    public List<SpringCloudCluster> clusters(boolean... force) {
        return this.getSubscriptions().stream().parallel()
                .flatMap(s -> clusters(s.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @Nonnull
    @Cacheable(cacheName = "asc/{}/clusters", key = "$subscriptionId", condition = "!(force&&force[0])")
    @AzureOperation(name = "springcloud|cluster.list.subscription", params = "subscriptionId", type = AzureOperation.Type.SERVICE)
    private List<SpringCloudCluster> clusters(@Nonnull String subscriptionId, boolean... force) {
        try {
            return getClient(subscriptionId).list().stream()
                    .map(this::cluster)
                    .collect(Collectors.toList());
        } catch (ManagementException e) {
            log.warn(String.format("failed to list spring cloud services of subscription(%s)", subscriptionId), e);
            if (HttpStatus.SC_FORBIDDEN == e.getResponse().getStatusCode()) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    @AzureOperation(name = "common|service.refresh", params = "this.name()", type = AzureOperation.Type.SERVICE)
    public void refresh() {
        this.clusters(true);
    }

    @Override
    public String name() {
        return "Spring Cloud";
    }

    @Cacheable(cacheName = "asc/{}/client", key = "$subscriptionId")
    @AzureOperation(name = "springcloud.get_client.subscription", params = "subscriptionId", type = AzureOperation.Type.SERVICE)
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
