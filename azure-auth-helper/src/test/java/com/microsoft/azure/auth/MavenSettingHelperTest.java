/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.google.common.base.Preconditions;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.MavenDecryptException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class MavenSettingHelperTest {
    @Test
    public void testGetAuthConfigurationFromServer() throws Exception {
        final DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        final String encryptedKey = cipher.encryptAndDecorate("hello", "fake_master_key");
        final String xml = "<configuration>\n" +
                "       <client>df4d03fa-135b-4b7b-932d-2f2ba6449792</client>\n" +
                "       <tenant>72f988bf-86f1-41af-91ab-2d7cd011db47</tenant>\n" +
                "       <key>%s</key>\n" +
                "       <environment>AZURE</environment>\n" +
                "   </configuration>";
        final Xpp3Dom pluginConfiguration = Xpp3DomBuilder.build(new ByteArrayInputStream(String.format(xml, encryptedKey).getBytes()), "UTF-8");

        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getConfiguration()).thenReturn(pluginConfiguration);
        final Settings mavenSettings = Mockito.mock(Settings.class);
        Mockito.when(mavenSettings.getServer("test1")).thenReturn(server);

        final MavenSession mavenSession = Mockito.mock(MavenSession.class);
        Mockito.when(mavenSession.getSettings()).thenReturn(mavenSettings);

        final SettingsDecrypter settingsDecrypter = newSettingsDecrypter(Paths.get("src/test/resources/maven/settings/settings-security.xml"));

        final AuthConfiguration auth = MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, "test1");
        assertEquals("72f988bf-86f1-41af-91ab-2d7cd011db47", auth.getTenant());
        assertEquals("df4d03fa-135b-4b7b-932d-2f2ba6449792", auth.getClient());
        assertEquals("AZURE", auth.getEnvironment());
        assertEquals("hello", auth.getKey());
    }

    @Test
    public void testGetAuthConfigurationFromServerCertificate() throws Exception {
        final DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        final String encryptedKey = cipher.encryptAndDecorate("hello", "fake_master_key");
        final String xml = "<configuration>\n" +
                "       <client>df4d03fa-135b-4b7b-932d-2f2ba6449792</client>\n" +
                "       <tenant>72f988bf-86f1-41af-91ab-2d7cd011db47</tenant>\n" +
                "       <certificate>C:\\Users\\user\\test.pem</certificate>\n" +
                "       <certificatePassword>%s</certificatePassword>\n" +
                "       <environment>AZURE</environment>\n" +
                "   </configuration>";
        final Xpp3Dom pluginConfiguration = Xpp3DomBuilder.build(new ByteArrayInputStream(String.format(xml, encryptedKey).getBytes()), "UTF-8");

        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getConfiguration()).thenReturn(pluginConfiguration);
        final Settings mavenSettings = Mockito.mock(Settings.class);
        Mockito.when(mavenSettings.getServer("test1")).thenReturn(server);

        final MavenSession mavenSession = Mockito.mock(MavenSession.class);
        Mockito.when(mavenSession.getSettings()).thenReturn(mavenSettings);

        final SettingsDecrypter settingsDecrypter = newSettingsDecrypter(Paths.get("src/test/resources/maven/settings/settings-security.xml"));

        final AuthConfiguration auth = MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, "test1");
        assertEquals("72f988bf-86f1-41af-91ab-2d7cd011db47", auth.getTenant());
        assertEquals("df4d03fa-135b-4b7b-932d-2f2ba6449792", auth.getClient());
        assertEquals("AZURE", auth.getEnvironment());
        assertNull(auth.getKey());
        assertEquals("C:\\Users\\user\\test.pem", auth.getCertificate());
        assertEquals("hello", auth.getCertificatePassword());
    }

    @Test
    public void testBadKey() throws Exception {
        final DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        final String encryptedKey = cipher.encryptAndDecorate("hello", "hello");
        final String xml = "<configuration>\n" +
                "       <client>df4d03fa-135b-4b7b-932d-2f2ba6449792</client>\n" +
                "       <tenant>72f988bf-86f1-41af-91ab-2d7cd011db47</tenant>\n" +
                "       <key>%s</key>\n" +
                "       <environment>AZURE</environment>\n" +
                "   </configuration>";
        final Xpp3Dom pluginConfiguration = Xpp3DomBuilder.build(new ByteArrayInputStream(String.format(xml, encryptedKey).getBytes()), "UTF-8");

        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getConfiguration()).thenReturn(pluginConfiguration);
        final Settings mavenSettings = Mockito.mock(Settings.class);
        Mockito.when(mavenSettings.getServer("test1")).thenReturn(server);

        final MavenSession mavenSession = Mockito.mock(MavenSession.class);
        Mockito.when(mavenSession.getSettings()).thenReturn(mavenSettings);

        final SettingsDecrypter settingsDecrypter = newSettingsDecrypter(Paths.get("src/test/resources/maven/settings/settings-security.xml"));
        try {
            MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, "test1");
            fail("Should throw MavenDecryptException when the key is not broken.    ");
        } catch (MavenDecryptException ex) {
            // expect
        }
    }

    @Test
    public void testBadServerId() throws Exception {
        final Server server = Mockito.mock(Server.class);
        final Settings mavenSettings = Mockito.mock(Settings.class);
        Mockito.when(mavenSettings.getServer("test1")).thenReturn(server);

        final MavenSession mavenSession = Mockito.mock(MavenSession.class);
        Mockito.when(mavenSession.getSettings()).thenReturn(mavenSettings);

        final SettingsDecrypter settingsDecrypter = newSettingsDecrypter(Paths.get("src/test/resources/maven/settings/settings-security.xml"));

        try {
            MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, "");
            fail("should throw IAE.");
        } catch (IllegalArgumentException ex) {
            // expected her
        }

        try {
            MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, null);
            fail("should throw IAE.");
        } catch (IllegalArgumentException ex) {
            // expected her
        }

        assertNull(MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, "test2"));
        assertNotNull(MavenSettingHelper.getAuthConfigurationFromServer(mavenSession, settingsDecrypter, "test1"));
    }

    /**
     * Create a new {@link SettingsDecrypter} for testing purposes.
     *
     * @param settingsSecurityFile absolute path to security-settings.xml
     * @return {@link SettingsDecrypter} built from settingsSecurityFile
     */
    public static SettingsDecrypter newSettingsDecrypter(Path settingsSecurityFile) {
        Preconditions.checkArgument(Files.isRegularFile(settingsSecurityFile));
        try {

            final DefaultPlexusCipher injectCypher = new DefaultPlexusCipher();

            final DefaultSecDispatcher injectedDispatcher = new DefaultSecDispatcher();
            injectedDispatcher.setConfigurationFile(settingsSecurityFile.toAbsolutePath().toString());
            TestHelper.setField(DefaultSecDispatcher.class, injectedDispatcher, "_cipher", injectCypher);

            final DefaultSettingsDecrypter settingsDecrypter = new DefaultSettingsDecrypter();
            TestHelper.setField(DefaultSettingsDecrypter.class, settingsDecrypter, "securityDispatcher", injectedDispatcher);
            return settingsDecrypter;
        } catch (Exception ex) {
            throw new IllegalStateException("Tests need to be rewritten: " + ex.getMessage(), ex);
        }
    }
}
