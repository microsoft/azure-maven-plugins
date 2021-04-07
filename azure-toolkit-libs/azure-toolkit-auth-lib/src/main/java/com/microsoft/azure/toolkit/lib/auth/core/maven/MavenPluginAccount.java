/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.maven;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.util.IdentityConstants;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.core.refresktoken.RefreshTokenTenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MavenPluginAccount extends Account {
    private AzureCredential mavenCredentials;
    private String refreshToken;
    @Override
    public AuthMethod getMethod() {
        return AuthMethod.AZURE_SECRET_FILE;
    }

    @Override
    protected boolean checkAvailableInner() {
        if (MavenLoginHelper.existsAzureSecretFile()) {
            try {
                mavenCredentials = MavenLoginHelper.readAzureCredentials(MavenLoginHelper.getAzureSecretFile());
                refreshToken = mavenCredentials != null ? mavenCredentials.getRefreshToken() : null;
                return StringUtils.isNotBlank(refreshToken);
            } catch (IOException e) {
                this.entity.setLastError(e);
            }
        }
        return false;
    }

    @Override
    protected TokenCredential createTokenCredential() {
        if (isAvailable() || checkAvailableInner()) {
            AzureEnvironment environment = AzureEnvironment.AZURE;
            this.entity.setEnvironment(environment);
            String authority = AzureEnvironmentUtils.getAuthority(environment);
            final RefreshTokenTenantCredential tenantCredential = new RefreshTokenTenantCredential(authority,
                    IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID, refreshToken);
            this.entity.setTenantCredential(tenantCredential);
            return tenantCredential;
        }
        throw new AzureToolkitAuthenticationException("Cannot get credential from maven login.");
    }
}
