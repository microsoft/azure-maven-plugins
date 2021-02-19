/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.refreshtoken;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.util.IdentityConstants;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RefreshTokenCredentialBuilder {
    private static final String VSCODE_CLIENT_ID = "aebc6443-996d-45c2-90f0-388ff96faa56";

    @NotNull
    public TokenCredential buildTokenCredential(AzureEnvironment env, String tenantId, String refreshToken) {
        return buildRefreshTokenCredentialInternal(env, IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID, tenantId, refreshToken);
    }

    @NotNull
    public TokenCredential buildVSCodeTokenCredential(AzureEnvironment env, String tenantId, String refreshToken) {
        return buildRefreshTokenCredentialInternal(env, VSCODE_CLIENT_ID, tenantId, refreshToken);
    }

    private TokenCredential buildRefreshTokenCredentialInternal(AzureEnvironment env, String clientId, String tenantId, String refreshToken) {
        return request -> Mono.fromCallable(() -> {
            AuthenticationResult result =
                    new RefreshTokenCredentialBuilder()
                            .authorize(env,
                                    clientId,
                                    StringUtils.firstNonBlank(tenantId, "common"),
                                    refreshToken,
                                    StringUtils.isBlank(tenantId) ? null : ScopeUtil.scopesToResource(request.getScopes())
                            );
            return RefreshTokenCredentialBuilder.fromAuthenticationResult(result);
        });
    }

    public static AccessToken fromAuthenticationResult(AuthenticationResult authenticationResult) {
        if (authenticationResult == null) {
            return null;
        }
        OffsetDateTime expiresOnDate = authenticationResult.getExpiresOnDate() == null ? OffsetDateTime.MAX :
                OffsetDateTime.ofInstant(authenticationResult.getExpiresOnDate().toInstant(), ZoneOffset.UTC);

        return new AccessToken(authenticationResult.getAccessToken(), expiresOnDate);
    }

    private static AuthenticationResult authorize(AzureEnvironment env,
                                                                       String clientId,
                                                                       String tenantId,
                                                                       String refreshToken,
                                                                       String resource) throws LoginFailureException, MalformedURLException {
        AuthenticationContext context;
        AuthenticationResult result;
        String authorityUrl = env.getActiveDirectoryEndpoint().replaceAll("/+$", "") + "/" + tenantId;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(authorityUrl, true,
                    service);
            Future<AuthenticationResult> future = context
                    .acquireTokenByRefreshToken(refreshToken, clientId, resource, null);
            result = future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new LoginFailureException(e.getMessage(), e);
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new LoginFailureException("authentication result was null");
        }
        return result;
    }
}
