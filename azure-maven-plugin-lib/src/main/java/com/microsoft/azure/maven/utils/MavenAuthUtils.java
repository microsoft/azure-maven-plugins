/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.AzureAuthManager;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.model.AuthType;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.auth.util.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.microsoft.azure.maven.auth.MavenSettingHelper.buildAuthConfigurationByServerId;

public class MavenAuthUtils {
    private static final String INVALID_AZURE_ENVIRONMENT = "Invalid environment string '%s', please replace it with one of " +
        "\"Azure\", \"AzureChina\", \"AzureGermany\", \"AzureUSGovernment\",.";

    public static com.microsoft.azure.tools.auth.model.AuthConfiguration convertToAuthConfiguration(MavenAuthConfiguration auth)
            throws InvalidConfigurationException {
        if (Objects.isNull(auth)) {
            return new com.microsoft.azure.tools.auth.model.AuthConfiguration();
        }
        final com.microsoft.azure.tools.auth.model.AuthConfiguration authConfiguration = new com.microsoft.azure.tools.auth.model.AuthConfiguration();
        authConfiguration.setClient(auth.getClient());
        authConfiguration.setTenant(auth.getTenant());
        authConfiguration.setCertificate(auth.getCertificate());
        authConfiguration.setCertificatePassword(auth.getCertificatePassword());
        authConfiguration.setKey(auth.getKey());
        authConfiguration.setHttpProxyHost(auth.getHttpProxyHost());
        final String authTypeStr = auth.getType();
        authConfiguration.setType(AuthType.parseAuthType(authTypeStr));
        authConfiguration.setEnvironment(AuthHelper.stringToAzureEnvironment(auth.getEnvironment()));
        if (Objects.nonNull(auth.getHttpProxyPort()) && !NumberUtils.isCreatable(auth.getHttpProxyPort())) {
            throw new InvalidConfigurationException(String.format("Invalid integer number for httpProxyPort: '%s'", auth.getHttpProxyPort()));
        }
        authConfiguration.setHttpProxyPort(NumberUtils.toInt(auth.getHttpProxyPort()));

        if (StringUtils.isNotBlank(auth.getEnvironment()) && Objects.isNull(authConfiguration.getEnvironment())) {
            throw new InvalidConfigurationException(String.format(INVALID_AZURE_ENVIRONMENT, auth.getEnvironment()));
        }
        // if user specify 'auto', and there are SP configuration errors, it will fail back to other auth types
        // if user doesn't specify any authType
        if (StringUtils.isBlank(auth.getType())) {
            if (!StringUtils.isAllBlank(auth.getCertificate(), auth.getKey(), auth.getCertificatePassword())) {
                ValidationUtil.validateMavenAuthConfiguration(authConfiguration);
            }
        } else if (authConfiguration.getType() == AuthType.SERVICE_PRINCIPAL) {
            ValidationUtil.validateMavenAuthConfiguration(authConfiguration);
        }
        return authConfiguration;
    }

    public static AzureCredentialWrapper login(MavenSession session, SettingsDecrypter settingsDecrypter, @Nonnull MavenAuthConfiguration auth)
            throws AzureExecutionException, MavenDecryptException {
        final String serverId = auth.getServerId();
        final com.microsoft.azure.tools.auth.model.AuthConfiguration authConfiguration;
        try {
            authConfiguration = MavenAuthUtils.convertToAuthConfiguration(StringUtils.isNotBlank(auth.getServerId()) ?
                    buildAuthConfigurationByServerId(session, settingsDecrypter, serverId) : auth);
        } catch (InvalidConfigurationException ex) {
            final String messagePostfix = StringUtils.isNotBlank(serverId) ? ("in server: '" + serverId + "' at maven settings.xml.")
                    : "in <auth> configuration.";
            throw new AzureExecutionException(String.format("%s %s", ex.getMessage(), messagePostfix));
        }
        return AzureAuthManager.getAzureCredentialWrapper(authConfiguration).toBlocking().value();
    }

}
