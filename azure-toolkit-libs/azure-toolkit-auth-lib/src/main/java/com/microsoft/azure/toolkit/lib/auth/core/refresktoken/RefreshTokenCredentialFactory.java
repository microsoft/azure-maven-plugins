/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.refresktoken;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RefreshTokenCredentialFactory {
    public static TokenCredential fromRefreshToken(AzureEnvironment env, String clientId, String tenantId, String refreshToken) {
        return buildRefreshTokenCredential(env, clientId, tenantId, refreshToken);
    }

    private static TokenCredential buildRefreshTokenCredential(AzureEnvironment env, String clientId, String tenantId, String refreshToken) {
        return request -> Mono.fromCallable(() -> {
            AuthenticationResult result =
                    authorize(env,
                            clientId,
                            StringUtils.firstNonBlank(tenantId, "common"),
                            refreshToken,
                            StringUtils.isBlank(tenantId) ? null : ScopeUtil.scopesToResource(request.getScopes())
                    );
            return fromAuthenticationResult(result);
        });
    }

    private static AccessToken fromAuthenticationResult(AuthenticationResult authenticationResult) {
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
