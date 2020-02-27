/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.MavenDecryptException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class MavenSettingHelper {
    /**
     * Get auth configuration from serverId.
     *
     * @param session           the maven session
     * @param settingsDecrypter the decrypter
     * @param serverId          the server id
     * @return the auth configuration
     * @throws MavenDecryptException when there are errors decrypting maven generated password
     */
    public static AuthConfiguration getAuthConfigurationFromServer(MavenSession session, SettingsDecrypter settingsDecrypter, String serverId)
            throws MavenDecryptException {
        if (StringUtils.isBlank(serverId)) {
            throw new IllegalArgumentException("Parameter 'serverId' cannot be null or empty.");
        }
        final Server server = session.getSettings().getServer(serverId);
        if (server == null) {
            return null;
        }
        final AuthConfiguration configurationFromServer = new AuthConfiguration();
        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return configurationFromServer;
        }

        configurationFromServer.setTenant(getConfiguration(configuration, "tenant"));
        configurationFromServer.setClient(getConfiguration(configuration, "client"));
        final String rawKey = getConfiguration(configuration, "key");
        configurationFromServer.setKey(isValueEncrypted(rawKey) ? decryptMavenProtectedValue(settingsDecrypter, "key", rawKey) : rawKey);
        configurationFromServer.setCertificate(getConfiguration(configuration, "certificate"));
        final String rawCertificatePassword = getConfiguration(configuration, "certificatePassword");
        configurationFromServer.setCertificatePassword(isValueEncrypted(rawCertificatePassword) ?
                decryptMavenProtectedValue(settingsDecrypter, "certificatePassword", rawCertificatePassword) :
                rawCertificatePassword);
        configurationFromServer.setEnvironment(getConfiguration(configuration, "environment"));
        configurationFromServer.setHttpProxyHost(getConfiguration(configuration, "httpProxyHost"));
        configurationFromServer.setHttpProxyPort(getConfiguration(configuration, "httpProxyPort"));
        configurationFromServer.setServerId(serverId);
        return configurationFromServer;
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param configuration the <i>Xpp3Dom</i> object representing the configuration in &gt;server&gt;'.
     * @param property           the property name
     * @return String value if property exists; otherwise, return null.
     */
    private static String getConfiguration(final Xpp3Dom configuration, final String property) {
        final Xpp3Dom node = configuration.getChild(property);
        return node == null ? null : node.getValue();
    }

    private static boolean isValueEncrypted(String value) {
        return value != null && value.startsWith("{") && value.endsWith("}");
    }

    private static String decryptMavenProtectedValue(SettingsDecrypter settingsDecrypter, String propertyName, String value) throws MavenDecryptException {
        final Server server = new Server();
        server.setPassword(value);
        final SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
        final SettingsDecryptionResult result = settingsDecrypter.decrypt(request);
        for (final SettingsProblem problem : result.getProblems()) {
            if (problem.getSeverity() == SettingsProblem.Severity.ERROR || problem.getSeverity() == SettingsProblem.Severity.FATAL) {
                // for java 8+, it is ok to use operator '+' for string concatenation
                throw new MavenDecryptException(propertyName, value, "Unable to decrypt value(" + value + ") from settings.xml: " + problem);
            }
        }
        return result.getServer().getPassword();
    }


    private MavenSettingHelper() {

    }
}
