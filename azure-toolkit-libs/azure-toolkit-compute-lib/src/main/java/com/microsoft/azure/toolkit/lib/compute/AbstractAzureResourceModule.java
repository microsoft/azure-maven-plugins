/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.compute;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appservice.models.LogLevel;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.resources.fluentcore.arm.AzureConfigurable;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.implementation.AzureConfigurableImpl;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractAzureResourceModule<T extends IAzureBaseResource> extends SubscriptionScoped<AbstractAzureResourceModule<T>>
        implements AzureService<T> {

    public AbstractAzureResourceModule(@NotNull Function<List<Subscription>, AbstractAzureResourceModule<T>> creator,
                                       @Nullable List<Subscription> subscriptions) {
        super(creator, subscriptions);
    }

    public AbstractAzureResourceModule(@NotNull Function<List<Subscription>, AbstractAzureResourceModule<T>> creator) {
        super(creator);
    }

    public List<T> list() {
        return getSubscriptions().stream().parallel()
                .flatMap(subscription -> list(subscription.getId()).stream())
                .collect(Collectors.toList());
    }

    @Nonnull
    public T get(@Nonnull final String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        return get(resourceId.subscriptionId(), resourceId.resourceGroupName(), resourceId.name());
    }

    @Nonnull
    public T get(@Nonnull final String resourceGroup, @Nonnull final String name) {
        return get(getDefaultSubscription().getId(), resourceGroup, name);
    }

    protected abstract List<T> list(@Nonnull final String subscriptionId);

    @Nonnull
    protected abstract T get(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name);

    protected static <R extends AzureConfigurable<R>> R getResourceManager(String subscriptionId, AzureConfigurableImpl<R> configurable) {
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        return configurable.withLogLevel(logLevel).withPolicy(getUserAgentPolicy(userAgent));
    }

    protected static AzureProfile getAzureProfile(final String subscriptionId) {
        final Account account = Azure.az(AzureAccount.class).account();
        return new AzureProfile(null, subscriptionId, account.getEnvironment());
    }

    protected static HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
