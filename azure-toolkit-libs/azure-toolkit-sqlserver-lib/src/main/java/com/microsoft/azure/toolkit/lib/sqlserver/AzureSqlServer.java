/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CustomLog
public class AzureSqlServer extends AbstractAzService<MicrosoftSqlServiceSubscription, SqlServerManager> {

    public AzureSqlServer() {
        super("Microsoft.Sql");
    }

    @Nonnull
    public MicrosoftSqlServerModule servers(@Nonnull String subscriptionId) {
        final MicrosoftSqlServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getServerModule();
    }

    @Nonnull
    public List<MicrosoftSqlServer> servers() {
        return this.list().stream().flatMap(m -> m.servers().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    @Override
    protected SqlServerManager loadResourceFromAzure(@Nonnull String subscriptionId, @Nullable String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return SqlServerManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlServiceSubscription newResource(@Nonnull SqlServerManager manager) {
        return new MicrosoftSqlServiceSubscription(manager, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SQL servers";
    }
}
