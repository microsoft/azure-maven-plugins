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
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

@Slf4j
public class ServicePrincipalAccount extends Account {
    @Getter
    private final AuthType type = AuthType.SERVICE_PRINCIPAL;

    public ServicePrincipalAccount(@Nonnull AuthConfiguration config) {
        super(config);
    }

    @Override
    public boolean checkAvailable() {
        final AuthConfiguration config = getConfig();
        final boolean available = !StringUtils.isAllBlank(config.getCertificate(), config.getCertificatePassword(), config.getKey());
        log.trace("Auth type ({}) is {}available.", TextUtils.cyan(this.getType().name()), available ? "" : TextUtils.yellow("NOT "));
        return available;
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
                .additionallyAllowedTenants("*")
                .tokenCachePersistenceOptions(getPersistenceOptions())
                .executorService(config.getExecutorService())
                .build() :
            new ClientSecretCredentialBuilder()
                .tenantId(config.getTenant())
                .clientId(config.getClient())
                .clientSecret(config.getKey())
                .additionallyAllowedTenants("*")
                .tokenCachePersistenceOptions(getPersistenceOptions())
                .executorService(config.getExecutorService())
                .build();
    }

    @Override
    public String getClientId() {
        return this.getConfig().getClient();
    }
}
