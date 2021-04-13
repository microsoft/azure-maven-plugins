/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TODO: this class is for internal use only.
 */
public class RefreshTokenTokenCredentialManager extends TokenCredentialManagerWithCache {

    public static Mono<TokenCredentialManager> createTokenCredentialManager(@Nonnull AzureEnvironment env,
                                                                            String clientId, @Nonnull TokenCredential credential) {
        return fromCredential(env, clientId, getRootAccessToken(env, credential));
    }

    public static Mono<TokenCredentialManager> createTokenCredentialManager(@Nonnull AzureEnvironment env,
                                                                            @Nonnull String clientId,
                                                                            String refreshToken) {
        RefreshTokenCredential credential =
                new RefreshTokenCredential(AzureEnvironmentUtils.getAuthority(env), clientId, "common", refreshToken);
        return fromCredential(env, clientId, getRootAccessToken(env, credential));
    }

    private static String getRefreshTokenFromMsalToken(MsalToken accessToken) {
        IAuthenticationResult result = accessToken.getAuthenticationResult();
        if (result == null) {
            return null;
        }

        String refreshTokenFromResult;
        try {
            refreshTokenFromResult = (String) FieldUtils.readField(result, "refreshToken", true);
        } catch (IllegalAccessException e) {
            throw new AzureToolkitAuthenticationException("Cannot read refreshToken from IAuthenticationResult.");
        }

        return refreshTokenFromResult;
    }

    public static TokenCredentialManager createFromRefreshToken(
            @Nonnull AzureEnvironment env,
            MsalToken token,
            String authority, String clientId) {
        String refreshToken = getRefreshTokenFromMsalToken(token);
        if (StringUtils.isBlank(refreshToken)) {
            throw new IllegalArgumentException("Cannot get refresh token from msal token.");
        }

        final TokenCredentialManager tokenCredentialManager = new TokenCredentialManagerWithCache();
        tokenCredentialManager.setEnv(env);
        tokenCredentialManager.setEmail(getEmailFromMsalToken(token));
        tokenCredentialManager.setCredentialSupplier(tenantId -> new RefreshTokenCredential(authority, clientId, tenantId, refreshToken));;
        tokenCredentialManager.setRootCredentialSupplier(() -> request -> Mono.just(token));;
        return tokenCredentialManager;
    }

    private static String getEmailFromMsalToken(MsalToken token) {
        IAuthenticationResult result = token.getAuthenticationResult();
        if (result != null && result.account() != null) {
            return result.account().username();
        }
        return null;
    }

    @NotNull
    private static Mono<TokenCredentialManager> fromCredential(@Nonnull AzureEnvironment env, @Nonnull String clientId, Mono<AccessToken> rootAccessToken) {
        Mono<AccessToken> mono = rootAccessToken;
        return mono.map(accessToken -> {
            if (accessToken instanceof MsalToken) {
                return createFromRefreshToken(env, (MsalToken) accessToken, AzureEnvironmentUtils.getAuthority(env), clientId);
            }
            throw new AzureToolkitAuthenticationException(
                    String.format("The credential(%s) is not a msal token.", accessToken.getClass().getSimpleName()));
        });
    }

    public static Mono<AccessToken> getRootAccessToken(@Nonnull AzureEnvironment env, @Nonnull TokenCredential credential) {
        TokenRequestContext tokenRequestContext = new TokenRequestContext()
                .addScopes(ScopeUtil.resourceToScopes(env.getManagementEndpoint()));
        return credential.getToken(tokenRequestContext);
    }

    @AllArgsConstructor
    static class RefreshTokenCredential implements TokenCredential {
        private final String authority;
        private final String clientId;
        private final String tenantId;
        private final String refreshToken;

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext context) {
            return Mono.just(authenticate(ScopeUtil.scopesToResource(context.getScopes())));
        }

        private AccessToken authenticate(String resource) {
            AuthenticationContext context;
            AuthenticationResult result;
            String authorityUrl = authority + "/" + tenantId;
            ExecutorService service = Executors.newFixedThreadPool(1);
            try {
                context = new AuthenticationContext(authorityUrl, true, service);
                Future<AuthenticationResult> future = context
                        .acquireTokenByRefreshToken(refreshToken, clientId, resource, null);
                result = future.get();
            } catch (ExecutionException | InterruptedException | MalformedURLException e) {
                throw new AzureToolkitAuthenticationException(
                        String.format("Cannot acquire token from refresh token due to error: %s", e.getMessage()), e);
            } finally {
                service.shutdown();
            }

            if (result == null) {
                throw new AzureToolkitAuthenticationException("Authentication result from acquireTokenByRefreshToken is null.");
            }
            return fromAuthenticationResult(result);
        }

        private AccessToken fromAuthenticationResult(AuthenticationResult authenticationResult) {
            if (authenticationResult == null) {
                return null;
            }
            if (authenticationResult.getExpiresOnDate() == null) {
                throw new AzureToolkitAuthenticationException("Cannot find expiration information from AuthenticationResult.");
            }
            OffsetDateTime expiresOnDate = OffsetDateTime.ofInstant(authenticationResult.getExpiresOnDate().toInstant(), ZoneOffset.UTC);

            return new AccessToken(authenticationResult.getAccessToken(), expiresOnDate);
        }
    }
}
