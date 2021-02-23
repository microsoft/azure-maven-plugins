/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.common;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialProvider;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;

import java.util.List;
import java.util.stream.Collectors;

public class CommonAccountEntityBuilder {
    public static boolean listTenants(AccountEntity accountEntity) {
        try {
            AzureProfile azureProfile = new AzureProfile(accountEntity.getEnvironment());
            TokenCredential credential = accountEntity.getCredentialBuilder().provideCredentialCommon();
            List<String> tenantIds = AzureResourceManager.authenticate(credential, azureProfile)
                    .tenants().list().stream().map(Tenant::tenantId).collect(Collectors.toList());
            accountEntity.setTenantIds(tenantIds);
            accountEntity.setAuthenticated(true);
            return true;
        } catch (ClientAuthenticationException ex) {
            accountEntity.setAuthenticated(false);
            accountEntity.setError(ex);
            return false;
        }
    }

    public static AccountEntity createAccountEntity(AuthMethod method) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setMethod(method);
        return accountEntity;
    }

    public static ICredentialProvider fromRefreshToken(AzureEnvironment env, String clientId, String refreshToken) {
        return new ICredentialProvider() {
            @Override
            public TokenCredential provideCredentialForTenant(String tenantId) {
                return new RefreshTokenCredentialBuilder().buildTokenCredential(env, clientId, tenantId, refreshToken);
            }

            @Override
            public TokenCredential provideCredentialCommon() {
                return new RefreshTokenCredentialBuilder().buildTokenCredential(env, clientId, null, refreshToken);
            }
        };
    }
}
