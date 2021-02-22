/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */


package com.microsoft.azure.toolkit.lib.auth.core.maven;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.google.common.base.MoreObjects;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.refreshtoken.RefreshTokenCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentV2Utils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class MavenLoginAccountEntityBuilder implements IAccountEntityBuilder {
    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setMethod(AuthMethod.AZURE_SECRET_FILE);
        accountEntity.setAuthenticated(false);

        if (!MavenLoginHelper.existsAzureSecretFile()) {
            return accountEntity;
        }
        try {
            AzureCredential credentials = MavenLoginHelper.readAzureCredentials(MavenLoginHelper.getAzureSecretFile());
            String envString = credentials.getEnvironment();

            accountEntity.setEnvironment(envString);
            if (StringUtils.isBlank(credentials.getRefreshToken())) {
                throw new LoginFailureException("Missing required 'refresh_token' from file:" + MavenLoginHelper.getAzureSecretFile());
            }
            accountEntity.setSelectedSubscriptionIds(Arrays.asList(credentials.getDefaultSubscription()));
            if (credentials.getUserInfo() != null) {
                accountEntity.setEmail(credentials.getUserInfo().getDisplayableId());
            }
            AzureEnvironment env = MoreObjects.firstNonNull(AzureEnvironmentV2Utils.stringToAzureEnvironment(envString), AzureEnvironment.AZURE);
            accountEntity.setCredentialBuilder(new ICredentialBuilder() {
                @Override
                public TokenCredential getCredentialWrapperForSubscription(SubscriptionEntity subscriptionEntity) {
                    Objects.requireNonNull(subscriptionEntity, "Parameter 'subscriptionEntity' cannot be null for building credentials.");
                    return new RefreshTokenCredentialBuilder().buildTokenCredential(env, subscriptionEntity.getTenantId(), credentials.getRefreshToken());
                }

                @Override
                public TokenCredential getCredentialForTenant(String tenantId) {
                    return new RefreshTokenCredentialBuilder().buildTokenCredential(env, tenantId, credentials.getRefreshToken());
                }

                @Override
                public TokenCredential getCredentialForListingTenants() {
                    return new RefreshTokenCredentialBuilder().buildTokenCredential(env, null, credentials.getRefreshToken());
                }
            });
            accountEntity.setAuthenticated(true);

        } catch (IOException | LoginFailureException ex) {
            accountEntity.setError(ex);
        }
        return accountEntity;
    }
}
