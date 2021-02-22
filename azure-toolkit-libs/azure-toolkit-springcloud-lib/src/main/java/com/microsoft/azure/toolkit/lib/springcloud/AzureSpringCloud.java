/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudClusterManager;
import com.microsoft.rest.LogLevel;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureSpringCloud implements AzureService {
    public AzureSpringCloud() {
    }

    @Nonnull
    public SpringCloudCluster cluster(@Nonnull SpringCloudClusterEntity cluster) {
        final AppPlatformManager client = this.getAppPlatformManager(cluster.getSubscriptionId());
        return new SpringCloudCluster(cluster, client);
    }

    public SpringCloudCluster cluster(String name) {
        return this.clusters().stream()
            .filter((s) -> Objects.equals(s.name(), name))
            .findAny().orElse(null);
    }

    public List<SpringCloudCluster> clusters() {
        return Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream()
            .map(s -> getAppPlatformManager(s.getId()))
            .map(SpringCloudClusterManager::new)
            .flatMap(m -> m.getAll().stream())
            .map(this::cluster)
            .collect(Collectors.toList());
    }

    @SneakyThrows
    private AppPlatformManager getAppPlatformManager(final String subscriptionId) {
        // TODO: cache AppPlatformManager since authenticate is slow.
        final AzureConfiguration config = Azure.az().config();
        final LogLevel logLevel = config.getLogLevel();
        final String userAgent = config.getUserAgent();
        return AppPlatformManager.configure()
            .withLogLevel(logLevel)
            .withUserAgent(userAgent)
            .authenticate(Azure.az(AzureAccount.class).account().getCredentialV1(subscriptionId), subscriptionId);
    }
}
