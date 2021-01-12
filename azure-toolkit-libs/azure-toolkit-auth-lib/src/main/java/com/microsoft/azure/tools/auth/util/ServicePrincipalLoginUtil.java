/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.util;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.exception.AzureLoginException;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import org.apache.commons.lang3.StringUtils;

public class ServicePrincipalLoginUtil {
    public static AzureCredentialWrapper login(AuthConfiguration configuration)
            throws AzureLoginException {
        // check maven configuration: client, tenant
        if (StringUtils.isNoneBlank(configuration.getClient(), configuration.getTenant())) {
            ValidationUtil.validateMavenAuthConfiguration(configuration);
            return mavenSettingLogin(AuthMethod.SERVICE_PRINCIPAL, configuration);
        }

        // if we cannot login through maven configuration, we should validate on user's
        // incomplete configuration
        if (!StringUtils.isAllBlank(configuration.getClient(), configuration.getTenant(),
                configuration.getCertificate(), configuration.getKey(), configuration.getCertificatePassword(),
                configuration.getHttpProxyHost(), configuration.getHttpProxyPort())) {
            throw new InvalidConfigurationException(
                    "Incomplete auth configurations in maven configuration, " +
                            "please refer to https://github.com/microsoft/azure-maven-plugins/wiki/Authentication");
        }
        return null;
    }

    private static AzureCredentialWrapper mavenSettingLogin(AuthMethod method, AuthConfiguration configuration) {
        AzureEnvironment env = AuthHelper.parseAzureEnvironment(configuration.getEnvironment());
        TokenCredential clientSecretCredential = StringUtils.isNotBlank(configuration.getCertificate()) ?
                new ClientCertificateCredentialBuilder().clientId(configuration.getClient())
                .pfxCertificate(configuration.getCertificate(), configuration.getCertificatePassword())
                .tenantId(configuration.getTenant()).build()

                : new ClientSecretCredentialBuilder().clientId(configuration.getClient())
                .clientSecret(configuration.getKey()).tenantId(configuration.getTenant()).build();

        return new AzureCredentialWrapper(method, clientSecretCredential, env == null ? AzureEnvironment.AZURE : env);
    }

}
