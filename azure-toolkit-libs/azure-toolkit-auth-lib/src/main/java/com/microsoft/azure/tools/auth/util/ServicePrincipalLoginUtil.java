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
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.exception.MavenDecryptException;
import com.microsoft.azure.tools.auth.maven.MavenSettingHelper;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.auth.model.MavenAuthConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import rx.Single;
import rx.exceptions.Exceptions;

public class ServicePrincipalLoginUtil {
    public static Single<AzureCredentialWrapper> login(MavenAuthConfiguration configuration, MavenSession session, SettingsDecrypter settingsDecrypter) {
        // 1. check maven configuration: server id
        try {
            if (StringUtils.isNotBlank(configuration.getServerId())) {
                MavenAuthConfiguration serverIdConfiguration = MavenSettingHelper.getAuthConfigurationFromServer(session, settingsDecrypter,
                        configuration.getServerId());
                return Single.just(mavenSettingLogin(AuthMethod.MAVEN_SETTINGS, serverIdConfiguration));
            }

            // 2. check maven configuration: client, tenant
            if (StringUtils.isNoneBlank(configuration.getClient(), configuration.getTenant())) {
                ValidationUtil.validateMavenAuthConfiguration(configuration);
                return Single.just(mavenSettingLogin(AuthMethod.MAVEN_CONFIGURATION, configuration));
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
            return Single.just(null);
        } catch (InvalidConfigurationException | MavenDecryptException ex) {
            throw Exceptions.propagate(ex);
        }
    }

    private static AzureCredentialWrapper mavenSettingLogin(AuthMethod method, MavenAuthConfiguration configuration) {
        AzureEnvironment env = AuthHelper.parseAzureEnvironment(configuration.getEnvironment());
        TokenCredential clientSecretCredential = StringUtils.isNotBlank(configuration.getCertificate()) ?
                new ClientCertificateCredentialBuilder().clientId(configuration.getClient()).pfxCertificate(configuration.getCertificate(), configuration.getCertificatePassword())
                .tenantId(configuration.getTenant()).build()

                : new ClientSecretCredentialBuilder().clientId(configuration.getClient())
                .clientSecret(configuration.getKey()).tenantId(configuration.getTenant()).build();

        return new AzureCredentialWrapper(method, clientSecretCredential, env == null ? AzureEnvironment.AZURE : env);
    }

}
