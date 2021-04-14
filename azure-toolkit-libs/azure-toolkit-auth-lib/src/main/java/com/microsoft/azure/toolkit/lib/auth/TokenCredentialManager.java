/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.FixedDelay;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class TokenCredentialManager implements TenantProvider, SubscriptionProvider {
    private static final ClientLogger LOGGER = new ClientLogger(TokenCredentialManager.class);

    @Setter
    @Getter
    protected AzureEnvironment environment;

    @Setter
    @Getter
    protected String email;

    @Setter
    protected Supplier<TokenCredential> rootCredentialSupplier;

    @Setter
    protected Function<String, TokenCredential> credentialSupplier;

    public TokenCredential createTokenCredentialForTenant(String tenantId) {
        return credentialSupplier.apply(tenantId);
    }

    public Mono<List<String>> listTenants() {
        return createAzureClient(environment).tenants().listAsync().map(Tenant::tenantId).collectList();
    }

    public Mono<List<Subscription>> listSubscriptions(List<String> tenantIds) {
        return Flux.fromIterable(tenantIds).parallel().runOn(Schedulers.boundedElastic())
                .flatMap(tenant -> listSubscriptionsInTenant(createAzureClient(environment, tenant), tenant)).sequential().collectList()
                .map(subscriptionsSet -> subscriptionsSet.stream()
                        .flatMap(Collection::stream)
                        .filter(Utils.distinctByKey(subscription -> StringUtils.lowerCase(subscription.getId())))
                        .collect(Collectors.toList()));
    }

    private static Mono<List<Subscription>> listSubscriptionsInTenant(AzureResourceManager.Authenticated client, String tenantId) {
        return client.subscriptions().listAsync()
                .map(s -> toSubscriptionEntity(tenantId, s)).collectList().onErrorResume(ex -> {
                    // warn and ignore, should modify here if IMessage is ready
                    LOGGER.warning(String.format("Cannot get subscriptions for tenant %s " +
                            ", please verify you have proper permissions over this tenant, detailed error: %s", tenantId, ex.getMessage()));
                    return Mono.just(new ArrayList<>());
                });
    }

    private static Subscription toSubscriptionEntity(String tenantId,
                                                     com.azure.resourcemanager.resources.models.Subscription subscription) {
        final Subscription subscriptionEntity = new Subscription();
        subscriptionEntity.setId(subscription.subscriptionId());
        subscriptionEntity.setName(subscription.displayName());
        subscriptionEntity.setTenantId(tenantId);
        return subscriptionEntity;
    }

    private AzureResourceManager.Authenticated createAzureClient(AzureEnvironment env, String tenantId) {
        AzureProfile profile = new AzureProfile(env);
        return configureAzure().authenticate(this.createTokenCredentialForTenant(tenantId), profile);
    }

    private AzureResourceManager.Authenticated createAzureClient(AzureEnvironment env) {
        AzureProfile profile = new AzureProfile(env);
        return configureAzure().authenticate(this.rootCredentialSupplier.get(), profile);
    }

    private static AzureResourceManager.Configurable configureAzure() {
        // disable retry for getting tenant and subscriptions
        return AzureResourceManager.configure()
                .withPolicy(createUserAgentPolicy())
                .withRetryPolicy(new RetryPolicy(new FixedDelay(0, Duration.ofSeconds(0))));
    }

    private static HttpPipelinePolicy createUserAgentPolicy() {
        final String userAgent = Azure.az().config().getUserAgent();
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
