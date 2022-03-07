/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.AzureConfigurable;
import com.azure.resourcemanager.resources.fluentcore.arm.Manager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractAzureResourceModule<T extends IAzureBaseResource> extends SubscriptionScoped<AbstractAzureResourceModule<T>>
        implements AzureService<T> {

    public AbstractAzureResourceModule(@Nonnull Function<List<Subscription>, AbstractAzureResourceModule<T>> creator,
                                       @Nullable List<Subscription> subscriptions) {
        super(creator, subscriptions);
    }

    public AbstractAzureResourceModule(@Nonnull Function<List<Subscription>, AbstractAzureResourceModule<T>> creator) {
        super(creator);
    }

    @Preload
    private static void preload() {
        Azure.getServices(AbstractAzureResourceModule.class).stream().parallel().forEach(AbstractAzureResourceModule::list);
    }

    public List<T> list(boolean... force) {
        return getSubscriptions().stream().parallel()
                .flatMap(subscription -> {
                    try {
                        return list(subscription.getId(), force).stream();
                    } catch (final RuntimeException e) {
                        AzureMessager.getMessager().warning(AzureString.format("%s : Failed to list resources in subscription %s",
                                this.getClass().getSimpleName(), subscription.getId()));
                        return Stream.empty();
                    }
                })
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

    public abstract List<T> list(@Nonnull final String subscriptionId, boolean... force);

    @Nonnull
    public abstract T get(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name);

    protected static <R extends AzureConfigurable<R>, T extends Manager> T getResourceManager(
            final String subscriptionId, Supplier<AzureConfigurable<R>> configurableSupplier, AuthenticationMethod<R, T> authenticationMethod) {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        final TokenCredential tokenCredential = account.getTokenCredential(subscriptionId);
        final R configurable = configurableSupplier.get().withPolicy(getUserAgentPolicy(userAgent)).withLogLevel(logLevel)
                .withHttpClient(AzureService.getDefaultHttpClient());
        return authenticationMethod.apply(configurable, tokenCredential, azureProfile);
    }

    @FunctionalInterface
    protected interface AuthenticationMethod<R extends AzureConfigurable<R>, T extends Manager> {
        T apply(R configurable, TokenCredential tokenCredential, AzureProfile azureProfile);
    }

    protected static HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
