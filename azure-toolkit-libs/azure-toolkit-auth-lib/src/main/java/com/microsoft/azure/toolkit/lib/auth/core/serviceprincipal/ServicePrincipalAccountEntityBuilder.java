/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.common.CommonAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.common.CommonCredentialProvider;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import org.apache.commons.lang3.StringUtils;

public class ServicePrincipalAccountEntityBuilder implements IAccountEntityBuilder {
    private AuthConfiguration configuration;

    public ServicePrincipalAccountEntityBuilder(AuthConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public AccountEntity build() {
        AzureEnvironmentUtils.setupAzureEnvironment(configuration.getEnvironment());
        TokenCredential clientSecretCredential = StringUtils.isNotBlank(configuration.getCertificate()) ?
                new ClientCertificateCredentialBuilder().clientId(configuration.getClient())
                        .pfxCertificate(configuration.getCertificate(), configuration.getCertificatePassword())
                        .tenantId(configuration.getTenant()).build()
                : new ClientSecretCredentialBuilder().clientId(configuration.getClient())
                .clientSecret(configuration.getKey()).tenantId(configuration.getTenant()).build();
        AccountEntity accountEntity = CommonAccountEntityBuilder.createAccountEntity(AuthMethod.SERVICE_PRINCIPAL);
        accountEntity.setEnvironment(configuration.getEnvironment());
        accountEntity.setCredentialBuilder(new CommonCredentialProvider(clientSecretCredential));
        if (!CommonAccountEntityBuilder.listTenants(accountEntity)) {
            return accountEntity;
        }

        return accountEntity;
    }
}
