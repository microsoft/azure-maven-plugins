/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.managedidentity;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.TokenCredentialManager;
import com.microsoft.azure.toolkit.lib.auth.TokenCredentialManagerWithCache;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class ManagedIdentityAccount extends Account {

    @Nullable
    private final AuthConfiguration config;

    public ManagedIdentityAccount(@Nullable final AuthConfiguration config) {
        this.config = config;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.MANAGED_IDENTITY;
    }

    @Override
    public String getClientId() {
        return Optional.ofNullable(this.config).map(AuthConfiguration::getClient).orElse(null);
    }

    protected Mono<Boolean> preLoginCheck() {
        // TODO: remove log error(from com.azure.core.implementation.AccessTokenCache#L153) when managed identity is not available
        return Mono.fromCallable(() -> this.initializeTokenCredentialManager()
                .map(TokenCredentialManager::listTenants).flatMapIterable(Mono::block)
                .count().blockOptional().filter(s -> s > 0).isPresent());
    }

    protected Mono<TokenCredentialManager> createTokenCredentialManager() {
        final AzureEnvironment env = Optional.ofNullable(this.config).map(AuthConfiguration::getEnvironment)
                .orElseGet(() -> Azure.az(AzureCloud.class).getOrDefault());
        return Mono.just(new ManagementIdentityTokenCredentialManager(env, this.getClientId()));
    }

    static class ManagementIdentityTokenCredentialManager extends TokenCredentialManagerWithCache {
        public ManagementIdentityTokenCredentialManager(@Nonnull AzureEnvironment environment, @Nullable String clientId) {
            this.environment = environment;
            rootCredentialSupplier = () -> new ManagedIdentityCredentialBuilder().clientId(clientId).build();
            credentialSupplier = (tenantId) -> new ManagedIdentityCredentialBuilder().clientId(clientId).build();
        }
    }
}
