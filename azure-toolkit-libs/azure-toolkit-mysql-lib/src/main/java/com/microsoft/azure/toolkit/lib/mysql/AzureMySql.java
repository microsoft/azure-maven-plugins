/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.ServerVersion;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AzureMySql extends AbstractAzResourceModule<MySqlResourceManager, AzResource.None, MySqlManager>
    implements AzService {

    public AzureMySql() {
        super("Microsoft.DBforMySQL", AzResource.NONE);
    }

    @Nonnull
    public MySqlServerModule servers(@Nonnull String subscriptionId) {
        final MySqlResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getServerModule();
    }

    public MySqlResourceManager forSubscription(@Nonnull String subscriptionId) {
        return this.get(subscriptionId, null);
    }

    @Nonnull
    @Override
    protected Stream<MySqlManager> loadResourcesFromAzure() {
        return Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream().parallel()
            .map(Subscription::getId).map(i -> loadResourceFromAzure(i, null));
    }

    @Nonnull
    @Override
    protected MySqlManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return MySqlManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
            .withPolicy(AzureService.getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Override
    protected MySqlResourceManager newResource(@Nonnull MySqlManager manager) {
        return new MySqlResourceManager(manager, this);
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/%s", resourceName, rg, this.getName());
    }

    public List<String> listSupportedVersions() {
        return ServerVersion.values().stream().map(ExpandableStringEnum::toString).collect(Collectors.toList());
    }
}
