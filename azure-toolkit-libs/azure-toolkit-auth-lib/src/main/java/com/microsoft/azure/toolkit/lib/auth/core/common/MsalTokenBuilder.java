/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.common;

import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.DeviceCodeInfo;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.SynchronizedAccessor;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.msal4j.DeviceCodeFlowParameters;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.Prompt;
import com.microsoft.aad.msal4j.PublicClientApplication;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MsalTokenBuilder {
    private final AzureEnvironment env;
    private final SynchronizedAccessor<PublicClientApplication> publicClientApplicationAccessor;
    @Getter
    private final String clientId;

    @Getter
    private final String tenantId = "organizations";

    public MsalTokenBuilder(AzureEnvironment env, String clientId) {
        this.env = env;
        this.clientId = clientId;
        this.publicClientApplicationAccessor = new SynchronizedAccessor<>(() -> createPublicClientApplication(clientId, tenantId, this.env));
    }

    public Mono<MsalToken> buildWithBrowserInteraction(int port) {
        URI redirectUri;
        String redirectUrl = "http://localhost:" + port;
        try {
            redirectUri = new URI(redirectUrl);
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
        Set<String> scopes = Arrays.stream(ScopeUtil.resourceToScopes(env.getManagementEndpoint())).collect(Collectors.toSet());
        InteractiveRequestParameters.InteractiveRequestParametersBuilder builder =
                InteractiveRequestParameters.builder(redirectUri).prompt(Prompt.SELECT_ACCOUNT).scopes(scopes);

        Mono<IAuthenticationResult> acquireToken = publicClientApplicationAccessor.getValue()
                .flatMap(pc -> Mono.fromFuture(() -> pc.acquireToken(builder.build())));

        return acquireToken.onErrorMap(t -> new ClientAuthenticationException(
                "Failed to acquire token with Interactive Browser Authentication.", null, t)).map(MsalToken::new);
    }

    public Mono<MsalToken> buildDeviceCode(Consumer<DeviceCodeInfo> deviceCodeConsumer) {
        Set<String> scopes = Arrays.stream(ScopeUtil.resourceToScopes(env.getManagementEndpoint())).collect(Collectors.toSet());
        return publicClientApplicationAccessor.getValue().flatMap(pc ->
                Mono.fromFuture(() -> {
                    DeviceCodeFlowParameters parameters = DeviceCodeFlowParameters.builder(scopes, dc -> deviceCodeConsumer.accept(
                                    new DeviceCodeInfo(dc.userCode(), dc.deviceCode(), dc.verificationUri(),
                                            OffsetDateTime.now().plusSeconds(dc.expiresIn()), dc.message()))).build();
                    return pc.acquireToken(parameters);
                }).onErrorMap(t -> new ClientAuthenticationException("Failed to acquire token with device code", null, t))
                        .map(MsalToken::new));

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
