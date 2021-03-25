/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.legacy;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.azure.identity.implementation.util.ScopeUtil;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class LegacyRefreshTokenCredentialFactory {
    public static Mono<TokenCredential> createRefreshTokenCredential(AzureEnvironment environment, TokenCredential tokenCredential) {
        return tokenCredential.getToken(new TokenRequestContext()
                .addScopes(ScopeUtil.resourceToScopes(environment.managementEndpoint())))
                .map(accessToken -> {
                    IAuthenticationResult result = ((MsalToken) accessToken).getAuthenticationResult();
                    String refreshTokenFromResult;
                    try {
                        refreshTokenFromResult = (String) FieldUtils.readField(result, "refreshToken", true);
                    } catch (IllegalAccessException e) {
                        throw new AzureToolkitRuntimeException("Cannot read refreshToken from IAuthenticationResult.");
                    }
                    if (StringUtils.isBlank(refreshTokenFromResult)) {
                        throw new AzureToolkitRuntimeException("Fail to get refresh token.");
                    }

                    List<String> tenantIds = listTenantIds(environment, tokenCredential);
                    if (CollectionUtils.isEmpty(tenantIds)) {
                        throw new AzureToolkitRuntimeException("There are no tenants in your account.");
                    }

                    return new RefreshTokenTokenCredential(
                            environment, IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID, tenantIds.get(0), refreshTokenFromResult);
                });
    }

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
        String authorityUrl = env.activeDirectoryEndpoint().replaceAll("/+$", "") + "/" + tenantId;
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

    private static List<String> listTenantIds(AzureEnvironment environment, TokenCredential credential) {
        com.azure.core.management.AzureEnvironment azureEnvironment =
                com.azure.core.management.AzureEnvironment.knownEnvironments().stream().filter(e ->
                        StringUtils.equals(e.getActiveDirectoryEndpoint(),
                                environment.activeDirectoryEndpoint())).findFirst().orElse(null);
        return AzureResourceManager.authenticate(credential
                , new AzureProfile(azureEnvironment)).tenants().list().stream().map(Tenant::tenantId)
                .collect(Collectors.toList());
    }
}
