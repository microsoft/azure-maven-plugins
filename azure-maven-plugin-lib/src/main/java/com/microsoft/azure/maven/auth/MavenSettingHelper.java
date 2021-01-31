/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Objects;

public class MavenSettingHelper {
    /**
     * Build maven auth configuration from serverId.
     *
     * @param session           the maven session
     * @param settingsDecrypter the decrypter
     * @param serverId          the server id
     * @return the auth configuration
     * @throws MavenDecryptException when there are errors decrypting maven generated password
     * @throws InvalidConfigurationException where are any illegal configurations
     */
    public static MavenAuthConfiguration buildAuthConfigurationByServerId(MavenSession session, SettingsDecrypter settingsDecrypter, String serverId)
            throws InvalidConfigurationException, MavenDecryptException {
        if (StringUtils.isBlank(serverId)) {
            throw new IllegalArgumentException("Parameter 'serverId' cannot be null or empty.");
        }
        final Server server = session.getSettings().getServer(serverId);
        if (server == null) {
            throw new InvalidConfigurationException(String.format("serverId '%s' cannot be found in maven settings.xml.", serverId));
        }
        final MavenAuthConfiguration configurationFromServer = new MavenAuthConfiguration();
        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        configurationFromServer.setServerId(serverId);
        if (configuration == null) {
            return configurationFromServer;
        }
        configurationFromServer.setTenant(getPropertyValue(configuration, "tenant"));
        configurationFromServer.setClient(getPropertyValue(configuration, "client"));
        final String rawKey = getPropertyValue(configuration, "key");
        configurationFromServer.setKey(isPropertyEncrypted(rawKey) ? decryptMavenSettingProperty(settingsDecrypter, "key", rawKey) : rawKey);
        configurationFromServer.setCertificate(getPropertyValue(configuration, "certificate"));
        final String rawCertificatePassword = getPropertyValue(configuration, "certificatePassword");
        configurationFromServer.setCertificatePassword(isPropertyEncrypted(rawCertificatePassword) ?
                decryptMavenSettingProperty(settingsDecrypter, "certificatePassword", rawCertificatePassword) :
                rawCertificatePassword);
        configurationFromServer.setEnvironment(getPropertyValue(configuration, "environment"));
        configurationFromServer.setServerId(serverId);
        return configurationFromServer;
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param configuration the <code>Xpp3Dom</code> object representing the configuration in &lt;server&gt;.
     * @param property           the property name
     * @return String value if property exists; otherwise, return null.
     */
    private static String getPropertyValue(final Xpp3Dom configuration, final String property) {
        final Xpp3Dom node = configuration.getChild(property);
        return Objects.isNull(node) ? null : node.getValue();
    }

    private static boolean isPropertyEncrypted(String value) {
        return value != null && value.startsWith("{") && value.endsWith("}");
    }

    private static String decryptMavenSettingProperty(SettingsDecrypter settingsDecrypter, String propertyName, String value) throws MavenDecryptException {
        final Server server = new Server();
        server.setPassword(value);
        final SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
        final SettingsDecryptionResult result = settingsDecrypter.decrypt(request);
        for (final SettingsProblem problem : result.getProblems()) {
            if (problem.getSeverity() == SettingsProblem.Severity.ERROR || problem.getSeverity() == SettingsProblem.Severity.FATAL) {
                // for java 8+, it is ok to use operator '+' for string concatenation
                throw new MavenDecryptException(String.format("Unable to decrypt property(%s), value(%s) from maven settings.xml due to error: %s",
                        propertyName, value, problem.toString()));
            }
        }
        return result.getServer().getPassword();
    }

    private MavenSettingHelper() {

    }
}
