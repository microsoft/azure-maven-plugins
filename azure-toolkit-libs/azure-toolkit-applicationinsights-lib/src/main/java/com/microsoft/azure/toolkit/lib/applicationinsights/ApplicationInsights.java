/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Providers;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationInsights extends SubscriptionScoped<ApplicationInsights> implements AzureService {
    public ApplicationInsights() { // for SPI
        super(ApplicationInsights::new);
    }

    private ApplicationInsights(@Nonnull final List<Subscription> subscriptions) {
        super(ApplicationInsights::new, subscriptions);
    }

    public boolean exists(@Nonnull String resourceGroup, @Nonnull String name) {
        return exists(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public boolean exists(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        try {
            return get(subscriptionId, resourceGroup, name) != null;
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            return false;
        }
    }

    public ApplicationInsightsEntity get(@Nonnull String resourceGroup, @Nonnull String name) {
        return get(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public ApplicationInsightsEntity get(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        return Optional.ofNullable(getApplicationInsightsManager(subscriptionId).components().getByResourceGroup(resourceGroup, name))
                .map(ApplicationInsights::getFromApplicationInsightsComponent)
                .orElse(null);
    }

    public ApplicationInsightsEntity create(@Nonnull String resourceGroup, @Nonnull Region region, @Nonnull String name) {
        return create(getDefaultSubscription().getId(), resourceGroup, region, name);
    }

    /**
     * Create application insights instance with kind:web and applicationType:web
     *
     * @param subscriptionId id for subscription to create the resource
     * @param resourceGroup  existing resource to create the resource
     * @param region         region to resource to create
     * @param name           name for resource to create
     * @return ApplicationInsightsEntity for created resource
     */
    public ApplicationInsightsEntity create(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull Region region, @Nonnull String name) {
        final ApplicationInsightsComponent component = getApplicationInsightsManager(subscriptionId).components().define(name)
                .withRegion(region.getName()).withExistingResourceGroup(resourceGroup).withKind("web").withApplicationType(ApplicationType.WEB).create();
        return getFromApplicationInsightsComponent(component);
    }

    public List<ApplicationInsightsEntity> list() {
        return getSubscriptions().stream().parallel()
                .map(subscription -> getApplicationInsightsManager(subscription.getId()))
                .flatMap(manager -> manager.components().list().stream())
                .map(ApplicationInsights::getFromApplicationInsightsComponent)
                .collect(Collectors.toList());
    }

    public void delete(@Nonnull String resourceGroup, @Nonnull String name) {
        delete(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public void delete(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        getApplicationInsightsManager(subscriptionId).components().deleteByResourceGroup(resourceGroup, name);
    }

    @Cacheable(cacheName = "applicationinsights/{}/manager", key = "$subscriptionId")
    private ApplicationInsightsManager getApplicationInsightsManager(String subscriptionId) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String tenantId = account.getSubscription(subscriptionId).getTenantId();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogOptions logOptions = new HttpLogOptions();
        logOptions.setLogLevel(Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE));
        final AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, account.getEnvironment());
        // todo: migrate resource provider related codes to common library
        final Providers providers = ResourceManager.configure()
                .withPolicy(getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId).providers();
        return ApplicationInsightsManager
                .configure()
                .withLogOptions(logOptions)
                .withPolicy(getUserAgentPolicy(userAgent))
                .withPolicy(new ProviderRegistrationPolicy(providers)) // add policy to auto register resource providers
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    private HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }

    private static ApplicationInsightsEntity getFromApplicationInsightsComponent(final ApplicationInsightsComponent component) {
        final ResourceId resourceId = ResourceId.fromString(component.id());
        return ApplicationInsightsEntity.builder()
                .id(component.id())
                .name(component.name())
                .subscriptionId(resourceId.subscriptionId())
                .resourceGroup(resourceId.resourceGroupName())
                .region(Region.fromName(component.location()))
                .instrumentationKey(component.instrumentationKey())
                .kind(component.kind())
                .type(component.type())
                .build();
    }
}
