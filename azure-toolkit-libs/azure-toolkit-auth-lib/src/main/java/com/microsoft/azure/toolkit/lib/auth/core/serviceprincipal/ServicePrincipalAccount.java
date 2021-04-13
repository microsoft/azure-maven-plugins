/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.SingleTenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ServicePrincipalAccount extends Account {
    private final AuthConfiguration configuration;

    public ServicePrincipalAccount(@Nonnull AuthConfiguration authConfiguration) {
        Objects.requireNonNull(authConfiguration);
        this.configuration = authConfiguration;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.SERVICE_PRINCIPAL;
    }

    @Override
    protected String getClientId() {
        return this.configuration.getClient();
    }

    protected Mono<Boolean> preLoginCheck() {
        return Mono.fromCallable(() -> {
            try {
                ValidationUtil.validateAuthConfiguration(configuration);
                return true;
            } catch (InvalidConfigurationException e) {
                throw new AzureToolkitAuthenticationException(
                        "Cannot login through 'SERVICE_PRINCIPAL' due to invalid configuration:" + e.getMessage());
            }
        });
    }

    protected TokenCredential createTokenCredential() {
        AzureEnvironmentUtils.setupAzureEnvironment(configuration.getEnvironment());
        this.entity.setEnvironment(configuration.getEnvironment());
        TokenCredential clientSecretCredential = StringUtils.isNotBlank(configuration.getCertificate()) ?
                new ClientCertificateCredentialBuilder().clientId(configuration.getClient())
                        .pfxCertificate(configuration.getCertificate(), configuration.getCertificatePassword())
                        .tenantId(configuration.getTenant()).build()
                : new ClientSecretCredentialBuilder().clientId(configuration.getClient())
                .clientSecret(configuration.getKey()).tenantId(configuration.getTenant()).build();
        this.entity.setTenantCredential(new SingleTenantCredential(clientSecretCredential));
        return clientSecretCredential;
    }
}
