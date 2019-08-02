/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
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
     * @throws InvalidConfigurationException when the configuration is invalid
     */
    public static AuthConfiguration loadFromMavenSettings(MavenSession session, SettingsDecrypter settingsDecrypter, String serverId)
            throws InvalidConfigurationException {
        if (StringUtils.isBlank(serverId)) {
            throw new IllegalArgumentException("Parameter 'serverId' cannot be empty.");
        }
        final Server originServer = session.getSettings().getServer(serverId);
        if (originServer == null) {
            throw new IllegalArgumentException(String.format("Cannot find %s in settings.xml.", serverId));
        }

        final SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(originServer);
        final String rawKey = getConfiguration(originServer, "key");
        final String rawCertificatePassword = getConfiguration(originServer, "certificatePassword");
        boolean isKeyEncrypted = false;
        boolean isCertificatePasswordEncrypted = false;
        if (isValueEncrypted(rawKey)) {
            originServer.setPassword(rawKey);
            isKeyEncrypted = true;
        } else if (isValueEncrypted(rawCertificatePassword)) {
            originServer.setPassword(rawCertificatePassword);
            isCertificatePasswordEncrypted = true;
        }
        final SettingsDecryptionResult result = settingsDecrypter.decrypt(request);
        for (final SettingsProblem problem : result.getProblems()) {
            if (problem.getSeverity() == SettingsProblem.Severity.ERROR || problem.getSeverity() == SettingsProblem.Severity.FATAL) {
                // for java 8+, it is ok to use operator '+' for string concatenation
                throw new InvalidConfigurationException("Unable to decrypt server(" + serverId + ") info from settings.xml: " + problem);
            }
        }
        final Server server = result.getServer();
        final AuthConfiguration conf = new AuthConfiguration();

        conf.setTenant(getConfiguration(server, "tenant"));
        conf.setClient(getConfiguration(server, "client"));
        conf.setKey(isKeyEncrypted ? server.getPassword() : rawKey);
        conf.setCertificate(getConfiguration(server, "certificate"));
        conf.setCertificatePassword(isCertificatePasswordEncrypted ? server.getPassword() : rawCertificatePassword);
        conf.setCertificatePassword(getConfiguration(server, "environment"));

        if (StringUtils.isBlank(conf.getTenant())) {
            throw new InvalidConfigurationException(String.format("Cannot find 'tenant' in settings.xml for sever: %s.", serverId));
        }
        if (StringUtils.isBlank(conf.getClient())) {
            throw new InvalidConfigurationException(String.format("Cannot find 'client' in settings.xml for sever: %s.", serverId));
        }

        if (StringUtils.isBlank(conf.getKey()) && StringUtils.isBlank(conf.getCertificate())) {
            throw new InvalidConfigurationException(
                    String.format("Cannot find either 'key' or 'certificate' in settings.xml for sever: %s.", serverId));
        }

        return conf;
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param server Server object.
     * @param key    Key string.
     * @return String value if key exists; otherwise, return null.
     */
    private static String getConfiguration(final Server server, final String key) {
        if (server == null) {
            return null;
        }

        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        final Xpp3Dom node = configuration.getChild(key);
        return node == null ? null : node.getValue();
    }

    private static boolean isValueEncrypted(String value) {
        return value != null && value.startsWith("{") && value.endsWith("}");
    }

    private MavenSettingHelper() {

    }
}
