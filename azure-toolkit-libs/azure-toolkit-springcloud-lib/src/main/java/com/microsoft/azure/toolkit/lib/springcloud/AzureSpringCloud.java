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

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudClusterManager;
import com.microsoft.rest.LogLevel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureSpringCloud implements AzureService {
    private final Account account;

    public AzureSpringCloud() {
        this.account = Azure.az(AzureAccount.class).account();
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
        return this.account.getSelectedSubscriptions().stream()
            .map(s -> getAppPlatformManager(s.getId()))
            .map(SpringCloudClusterManager::new)
            .flatMap(m -> m.getAll().stream())
            .map(this::cluster)
            .collect(Collectors.toList());
    }

    private AppPlatformManager getAppPlatformManager(final String subscriptionId) {
        // TODO: cache AppPlatformManager since authenticate is slow.
        final AzureConfiguration config = Azure.az().config();
        final LogLevel logLevel = config.getLogLevel();
        final String userAgent = config.getUserAgent();
        return AppPlatformManager.configure()
            .withLogLevel(logLevel)
            .withUserAgent(userAgent)
            .authenticate(this.account.getTokenCredentialV1(subscriptionId), subscriptionId);
    }
}
