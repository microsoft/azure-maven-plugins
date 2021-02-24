/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.common.DefaultCredentialProvider;
import com.microsoft.azure.toolkit.lib.auth.util.AccountBuilderUtils;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
public class ServicePrincipalAccountEntityBuilder implements IAccountEntityBuilder {
    private AuthConfiguration configuration;

    @Override
    public AccountEntity build() {
        TokenCredential clientSecretCredential = StringUtils.isNotBlank(configuration.getCertificate()) ?
                new ClientCertificateCredentialBuilder().clientId(configuration.getClient())
                        .pfxCertificate(configuration.getCertificate(), configuration.getCertificatePassword())
                        .tenantId(configuration.getTenant()).build()
                : new ClientSecretCredentialBuilder().clientId(configuration.getClient())
                .clientSecret(configuration.getKey()).tenantId(configuration.getTenant()).build();
        AccountEntity accountEntity = AccountBuilderUtils.createAccountEntity(AuthMethod.SERVICE_PRINCIPAL);
        accountEntity.setEnvironment(configuration.getEnvironment());
        accountEntity.setCredentialBuilder(new DefaultCredentialProvider(clientSecretCredential));
        AccountBuilderUtils.listTenants(accountEntity);
        return accountEntity;
    }
}
