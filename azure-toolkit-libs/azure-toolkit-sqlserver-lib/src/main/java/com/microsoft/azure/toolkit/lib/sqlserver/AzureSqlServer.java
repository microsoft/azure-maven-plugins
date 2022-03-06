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
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
public class AzureSqlServer extends AbstractAzService<MicrosoftSqlResourceManager, SqlServerManager> {

    public AzureSqlServer() {
        super("Microsoft.SQL");
    }

    @Nonnull
    public MicrosoftSqlServerModule servers(@Nonnull String subscriptionId) {
        final MicrosoftSqlResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getServerModule();
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
            .withHttpClient(AbstractAzResourceManager.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
            .withPolicy(AbstractAzResourceManager.getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlResourceManager newResource(@Nonnull SqlServerManager manager) {
        return new MicrosoftSqlResourceManager(manager, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SQL servers";
    }
}
