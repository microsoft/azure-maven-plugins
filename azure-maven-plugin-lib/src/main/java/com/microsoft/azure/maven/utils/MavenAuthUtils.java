/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.microsoft.azure.maven.auth.MavenSettingHelper.buildAuthConfigurationByServerId;

public class MavenAuthUtils {
    private static final String INVALID_AZURE_ENVIRONMENT = "Invalid environment string '%s', please replace it with one of " +
            "\"Azure\", \"AzureChina\", \"AzureGermany\", \"AzureUSGovernment\",.";

    public static AuthConfiguration buildAuthConfiguration(MavenSession session, SettingsDecrypter settingsDecrypter, @Nonnull MavenAuthConfiguration auth)
            throws AzureExecutionException, MavenDecryptException {
        final String serverId = auth.getServerId();
        final AuthConfiguration authConfiguration;
        try {
            authConfiguration = convertToAuthConfiguration(StringUtils.isNotBlank(auth.getServerId()) ?
                    buildAuthConfigurationByServerId(session, settingsDecrypter, serverId) : auth);
        } catch (InvalidConfigurationException ex) {
            final String messagePostfix = StringUtils.isNotBlank(serverId) ? ("in server: '" + serverId + "' at maven settings.xml.")
                    : "in <auth> configuration.";
            throw new AzureExecutionException(String.format("%s %s", ex.getMessage(), messagePostfix));
        }
        return authConfiguration;
    }

    private static AuthConfiguration convertToAuthConfiguration(MavenAuthConfiguration mavenAuthConfiguration)
            throws InvalidConfigurationException {
        if (Objects.isNull(mavenAuthConfiguration)) {
            return new AuthConfiguration();
        }
        final AuthConfiguration authConfiguration = new AuthConfiguration();
        authConfiguration.setClient(mavenAuthConfiguration.getClient());
        authConfiguration.setTenant(mavenAuthConfiguration.getTenant());
        authConfiguration.setCertificate(mavenAuthConfiguration.getCertificate());
        authConfiguration.setCertificatePassword(mavenAuthConfiguration.getCertificatePassword());
        authConfiguration.setKey(mavenAuthConfiguration.getKey());

        final String authTypeStr = mavenAuthConfiguration.getType();
        authConfiguration.setType(AuthType.parseAuthType(authTypeStr));

        authConfiguration.setEnvironment(AzureEnvironmentUtils.stringToAzureEnvironment(mavenAuthConfiguration.getEnvironment()));
        if (StringUtils.isNotBlank(mavenAuthConfiguration.getEnvironment()) && Objects.isNull(authConfiguration.getEnvironment())) {
            throw new InvalidConfigurationException(String.format(INVALID_AZURE_ENVIRONMENT, mavenAuthConfiguration.getEnvironment()));
        }

        // if user specify 'auto', and there are SP configuration errors, it will fail back to other auth types
        // if user doesn't specify any authType
        if (StringUtils.isBlank(mavenAuthConfiguration.getType())) {
            if (!StringUtils.isAllBlank(mavenAuthConfiguration.getCertificate(), mavenAuthConfiguration.getKey(),
                    mavenAuthConfiguration.getCertificatePassword())) {
                ValidationUtil.validateAuthConfiguration(authConfiguration);
            }
        } else if (authConfiguration.getType() == AuthType.SERVICE_PRINCIPAL) {
            ValidationUtil.validateAuthConfiguration(authConfiguration);
        }

        return authConfiguration;
    }

    public static void disableIdentityLogs() {
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.identity", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.adal4j", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.core.credential", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.msal4jextensions", "off");
    }

    private static void putPropertyIfNotExist(String key, String value) {
        if (StringUtils.isBlank(System.getProperty(key))) {
            System.setProperty(key, value);
        }
    }
}
