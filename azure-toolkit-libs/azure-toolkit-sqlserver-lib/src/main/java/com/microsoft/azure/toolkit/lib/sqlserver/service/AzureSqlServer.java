/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.impl.SqlServer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureSqlServer extends SubscriptionScoped<AzureSqlServer> implements AzureService {

    public AzureSqlServer() {
        super(AzureSqlServer::new);
    }

    private AzureSqlServer(@Nonnull final List<Subscription> subscriptions) {
        super(AzureSqlServer::new, subscriptions);
    }

    public ISqlServer sqlServer(String id) {
        final SqlServerEntity entity = SqlServerEntity.builder().id(id).build();
        return sqlServer(entity);
    }

    public ISqlServer sqlServer(String subscriptionId, String resourceGroup, String name) {
        final SqlServerEntity entity = SqlServerEntity.builder().subscriptionId(subscriptionId).resourceGroup(resourceGroup).name(name).build();
        return sqlServer(entity);
    }

    public ISqlServer sqlServer(SqlServerEntity entity) {
        final String subscriptionId = getSubscriptionFromResourceEntity(entity);
        return new SqlServer(entity, getAzureResourceManager(subscriptionId));
    }

    private ISqlServer sqlServer(com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
        return new SqlServer(sqlServerInner, getAzureResourceManager(sqlServerInner.manager().subscriptionId()));
    }

    public List<ISqlServer> sqlServers() {
        return getSubscriptions().stream()
                .map(subscription -> getAzureResourceManager(subscription.getId()))
                .flatMap(azureResourceManager -> azureResourceManager.sqlServers().list().stream())
                .collect(Collectors.toList()).stream()
                .map(server -> sqlServer(server))
                .collect(Collectors.toList());
    }

    // todo: share codes with other library which leverage track2 mgmt sdk
    @Cacheable(cacheName = "AzureResourceManager", key = "$subscriptionId")
    public AzureResourceManager getAzureResourceManager(String subscriptionId) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(account.getEnvironment());
        return AzureResourceManager.configure()
                .withLogLevel(logLevel)
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

    private String getSubscriptionFromResourceEntity(@Nonnull IAzureResourceEntity resourceEntity) {
        if (StringUtils.isNotEmpty(resourceEntity.getId())) {
            return ResourceId.fromString(resourceEntity.getId()).subscriptionId();
        }
        if (StringUtils.isNotEmpty(resourceEntity.getSubscriptionId())) {
            return resourceEntity.getSubscriptionId();
        }
        throw new AzureToolkitRuntimeException("Subscription id is required for this request.");
    }
}
