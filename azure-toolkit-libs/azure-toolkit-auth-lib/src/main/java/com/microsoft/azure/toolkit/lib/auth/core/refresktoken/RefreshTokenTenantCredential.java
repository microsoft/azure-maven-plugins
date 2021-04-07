/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.refresktoken;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.TenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.*;

@AllArgsConstructor
public class RefreshTokenTenantCredential extends TenantCredential {
    private final Map<String, TokenCredential> tokenCredentialCache = new ConcurrentHashMap<>();
    private final String authority;
    private final String clientId;
    private final String refreshToken;

    @Override
    protected Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext context) {
        final String key = StringUtils.firstNonBlank(tenantId, "$");
        if (!tokenCredentialCache.containsKey(key)) {
            tokenCredentialCache.put(key, createTokenCredential(tenantId));
        }
        return tokenCredentialCache.get(key).getToken(context);
    }

    private TokenCredential createTokenCredential(String tenantId) {
        return request -> Mono.fromCallable(() -> {
            AuthenticationResult result = authenticate(StringUtils.firstNonBlank(tenantId, "common"),
                            ScopeUtil.scopesToResource(request.getScopes()));
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

    private AuthenticationResult authenticate(String tenantId, String resource) throws LoginFailureException, MalformedURLException {
        AuthenticationContext context;
        AuthenticationResult result;
        String authorityUrl = authority + "/" + tenantId;
        ExecutorService service = Executors.newFixedThreadPool(1);
        try {
            context = new AuthenticationContext(authorityUrl, true, service);
            Future<AuthenticationResult> future = context
                    .acquireTokenByRefreshToken(refreshToken, clientId, resource, null);
            result = future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new LoginFailureException(e.getMessage(), e);
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new LoginFailureException("Authentication result from acquireTokenByRefreshToken is null.");
        }
        return result;
    }
}
