/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design.sp;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.design.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.design.AzResource;
import com.microsoft.azure.toolkit.lib.design.AzService;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public final class AzureSpringCloud extends AbstractAzResourceModule<SpringCloudResourceManager, AzResource.None, AppPlatformManager> implements AzService {
    public AzureSpringCloud() {
        super("Microsoft.AppPlatform", AzResource.NONE); // for SPI
    }

    public SpringCloudClusterModule clusters(String subscriptionId) {
        final SpringCloudResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getClusterModule();
    }

    protected Stream<AppPlatformManager> loadResourcesFromAzure() {
        return Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream().parallel()
            .map(Subscription::getId).map(this::loadResourceFromAzure);
    }

    @Override
    protected AppPlatformManager loadResourceFromAzure(@Nonnull String resourceId) {
        final String subscriptionId = ResourceId.fromString(resourceId).subscriptionId();
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return AppPlatformManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
            .withLogLevel(logLevel)
            .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Override
    protected AppPlatformManager createResourceInAzure(String name, String resourceGroup, Object config) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    protected AppPlatformManager updateResourceInAzure(AppPlatformManager remote, Object config) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    protected SpringCloudResourceManager createNewResource(String name, String resourceGroup, Object config) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    protected SpringCloudResourceManager wrap(AppPlatformManager remote) {
        return new SpringCloudResourceManager(remote, this);
    }

    @Override
    protected Object getClient() {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String name, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/%s", name, rg, this.getName());
    }

    private static HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
