/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class ServicePrincipalCredentialRetriever extends AbstractCredentialRetriever {
    @Getter
    private AuthConfiguration auth;

    public ServicePrincipalCredentialRetriever(@Nonnull AuthConfiguration auth) {
        super(auth.getEnvironment());
        this.auth = auth;
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        try {
            ValidationUtil.validateAuthConfiguration(auth);
        } catch (InvalidConfigurationException e) {
            throw new LoginFailureException(e.getMessage());
        }
        return mavenSettingLogin(AuthMethod.SERVICE_PRINCIPAL, auth);
    }

    private AzureCredentialWrapper mavenSettingLogin(AuthMethod method, AuthConfiguration configuration) throws LoginFailureException {
        TokenCredential clientSecretCredential = StringUtils.isNotBlank(configuration.getCertificate()) ?
            new ClientCertificateCredentialBuilder().clientId(configuration.getClient())
                .pfxCertificate(configuration.getCertificate(), configuration.getCertificatePassword())
                .tenantId(configuration.getTenant()).build()
            : new ClientSecretCredentialBuilder().clientId(configuration.getClient())
            .clientSecret(configuration.getKey()).tenantId(configuration.getTenant()).build();
        validateTokenCredential(clientSecretCredential);
        return new AzureCredentialWrapper(method, clientSecretCredential, getAzureEnvironment()).withTenantId(configuration.getTenant());
    }

}
