/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.refresktoken;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.BaseTokenCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.core.publisher.Mono;

public abstract class RefreshTokenAccount extends Account {
    @Getter
    protected AzureEnvironment environment;

    protected String refreshToken;

    protected String clientId = IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID;

    protected abstract void initializeRefreshToken();

    protected Mono<Boolean> checkAvailableInner() {
        return Mono.fromCallable(() -> {
            initializeRefreshToken();
            return StringUtils.isNotEmpty(refreshToken);
        });
    }

    @Override
    protected void initializeCredentials() throws LoginFailureException {
        initializeFromRefreshToken(refreshToken);
    }

    protected void initializeFromTokenCredential(TokenCredential tokenCredential) throws LoginFailureException {
        AccessToken accessToken = tokenCredential.getToken(new TokenRequestContext()
                .addScopes(ScopeUtil.resourceToScopes(environment.getManagementEndpoint()))).block();

        // legacy code will be removed after https://github.com/jongio/azidext/pull/41 is merged
        IAuthenticationResult result = ((MsalToken) accessToken).getAuthenticationResult();
        if (result != null && result.account() != null) {
            entity.setEmail(result.account().username());
        }
        String refreshTokenFromResult;
        try {
            refreshTokenFromResult = (String) FieldUtils.readField(result, "refreshToken", true);
        } catch (IllegalAccessException e) {
            throw new LoginFailureException("Cannot read refreshToken from IAuthenticationResult.");
        }
        if (StringUtils.isBlank(refreshTokenFromResult)) {
            throw new LoginFailureException("Fail to get refresh token.");
        }

        initializeFromRefreshToken(refreshTokenFromResult);
    }

    private void initializeFromRefreshToken(String refreshToken) {
        BaseTokenCredential refreshTokenCredential =
                new RefreshTokenTokenCredential(environment, clientId, refreshToken);
        entity.setCredential(refreshTokenCredential);
    }

}
