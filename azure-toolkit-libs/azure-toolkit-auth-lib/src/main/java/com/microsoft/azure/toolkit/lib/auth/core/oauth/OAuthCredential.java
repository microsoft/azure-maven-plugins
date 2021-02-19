/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.SynchronizedAccessor;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.Prompt;
import com.microsoft.aad.msal4j.PublicClientApplication;
import lombok.Getter;
import lombok.SneakyThrows;
import me.alexpanov.net.FreePortFinder;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class OAuthCredential implements TokenCredential {
    private final AzureEnvironment env;
    private final SynchronizedAccessor<PublicClientApplication> publicClientApplicationAccessor;
    @Getter
    private final String clientId;

    @Getter
    private final String tenantId = "organizations";

    public OAuthCredential(@NotNull AzureEnvironment env, @NotNull String clientId) {
        this.env = env;
        this.clientId = clientId;
        this.publicClientApplicationAccessor = new SynchronizedAccessor<>(() -> createPublicClientApplication(clientId, tenantId, this.env));

    }

    @SneakyThrows
    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        int port = FreePortFinder.findFreeLocalPort();
        return Mono.defer(() -> authenticateWithBrowserInteraction(request, port));
    }

    /**
     * Asynchronously acquire a token from Active Directory by opening a browser and wait for the user to login. The
     * credential will run a minimal local HttpServer at the given port, so {@code http://localhost:{port}} must be
     * listed as a valid reply URL for the application.
     *
     * @param request the details of the token request
     * @param port the port on which the HTTP server is listening
     * @return a Publisher that emits an AccessToken
     */
    public Mono<MsalToken> authenticateWithBrowserInteraction(TokenRequestContext request, int port) {
        URI redirectUri;
        String redirectUrl = "http://localhost:" + port;
        try {
            redirectUri = new URI(redirectUrl);
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
        InteractiveRequestParameters.InteractiveRequestParametersBuilder builder =
                InteractiveRequestParameters.builder(redirectUri).prompt(Prompt.SELECT_ACCOUNT).scopes(new HashSet<>(request.getScopes()));

        Mono<IAuthenticationResult> acquireToken = publicClientApplicationAccessor.getValue()
                .flatMap(pc -> Mono.fromFuture(() -> pc.acquireToken(builder.build())));

        return acquireToken.onErrorMap(t -> new ClientAuthenticationException(
                "Failed to acquire token with Interactive Browser Authentication.", null, t)).map(MsalToken::new);
    }

    @SneakyThrows
    private static PublicClientApplication createPublicClientApplication(String clientId, String tenantId, @NotNull AzureEnvironment env) {
        String authorityUrl = env.getActiveDirectoryEndpoint().replaceAll("/+$", "") + "/" + tenantId;
        Set<String> set = new HashSet<>(1);
        set.add("CP1");
        return PublicClientApplication
                .builder(clientId)
                .clientCapabilities(set)
                .authority(authorityUrl).build();
    }
}
