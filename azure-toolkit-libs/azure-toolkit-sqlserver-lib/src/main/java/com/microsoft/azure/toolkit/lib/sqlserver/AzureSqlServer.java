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
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Log4j2
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
    protected SqlServerManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return SqlServerManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
            .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
            .withPolicy(AzureService.getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Override
    protected MicrosoftSqlResourceManager newResource(@Nonnull SqlServerManager manager) {
        return new MicrosoftSqlResourceManager(manager, this);
    }

    @Override
    public String getResourceTypeName() {
        return "SQL servers";
    }
}
