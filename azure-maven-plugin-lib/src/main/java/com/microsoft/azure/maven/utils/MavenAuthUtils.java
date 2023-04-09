/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

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
            throw new AzureExecutionException(ex.getMessage());
        }
        return authConfiguration;
    }

    private static AuthConfiguration convertToAuthConfiguration(MavenAuthConfiguration mavenAuthConfiguration)
            throws InvalidConfigurationException {
        if (Objects.isNull(mavenAuthConfiguration)) {
            return new AuthConfiguration(AuthType.AUTO);
        }
        final AuthType type = AuthType.parseAuthType(mavenAuthConfiguration.getType());
        final AuthConfiguration authConfiguration = new AuthConfiguration(type);
        authConfiguration.setClient(mavenAuthConfiguration.getClient());
        authConfiguration.setTenant(mavenAuthConfiguration.getTenant());
        authConfiguration.setCertificate(mavenAuthConfiguration.getCertificate());
        authConfiguration.setCertificatePassword(mavenAuthConfiguration.getCertificatePassword());
        authConfiguration.setKey(mavenAuthConfiguration.getKey());

        authConfiguration.setEnvironment(mavenAuthConfiguration.getEnvironment());
        if (StringUtils.isNotBlank(mavenAuthConfiguration.getEnvironment()) && Objects.isNull(authConfiguration.getEnvironment())) {
            throw new InvalidConfigurationException(String.format(INVALID_AZURE_ENVIRONMENT, mavenAuthConfiguration.getEnvironment()));
        }
        authConfiguration.setEnvironment(Optional.ofNullable(authConfiguration.getEnvironment())
            .orElseGet(() -> AzureEnvironmentUtils.azureEnvironmentToString(AzureEnvironment.AZURE)));

        // if user specify 'auto', and there are SP configuration errors, it will fail back to other auth types
        // if user doesn't specify any authType
        if (StringUtils.isBlank(mavenAuthConfiguration.getType())) {
            if (!StringUtils.isAllBlank(mavenAuthConfiguration.getCertificate(), mavenAuthConfiguration.getKey(),
                mavenAuthConfiguration.getCertificatePassword())) {
                authConfiguration.validate();
            }
        } else if (authConfiguration.getType() == AuthType.SERVICE_PRINCIPAL) {
            authConfiguration.validate();
        }

        return authConfiguration;
    }

    /**
     * disable unexpected logs from azure identity
     */
    public static void disableIdentityLogs() {
        // add breakpoint on `org.slf4j.Logger.error` to track the logged errors.
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.identity", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.identity.implementation", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.msal4j", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.core.http.policy", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.core.implementation", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.adal4j", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.core.credential", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.msal4jextensions", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.core.implementation", "warn");
    }

    private static void putPropertyIfNotExist(String key, String value) {
        if (StringUtils.isBlank(System.getProperty(key))) {
            System.setProperty(key, value);
        }
    }
}
