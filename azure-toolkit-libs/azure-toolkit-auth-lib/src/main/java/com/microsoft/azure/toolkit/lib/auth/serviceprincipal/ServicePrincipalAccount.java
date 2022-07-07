/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class ServicePrincipalAccount extends Account {
    @Getter
    private final AuthType type = AuthType.SERVICE_PRINCIPAL;

    public ServicePrincipalAccount(@Nonnull AuthConfiguration config) {
        super(config);
    }

    @Override
    public boolean checkAvailable() {
        final AuthConfiguration config = getConfig();
        return !StringUtils.isAllBlank(config.getCertificate(), config.getCertificatePassword(), config.getKey());
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        final AuthConfiguration config = getConfig();
        return StringUtils.isNotBlank(config.getCertificate()) ?
            new ClientCertificateCredentialBuilder()
                .tenantId(config.getTenant())
                .clientId(config.getClient())
                .pfxCertificate(config.getCertificate(), config.getCertificatePassword())
                .build() :
            new ClientSecretCredentialBuilder()
                .tenantId(config.getTenant())
                .clientId(config.getClient())
                .clientSecret(config.getKey())
                .build();
    }

    @Override
    public String getClientId() {
        return this.getConfig().getClient();
    }
}
