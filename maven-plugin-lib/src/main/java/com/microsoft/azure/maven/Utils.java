/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * Utility class
 */
public final class Utils {
    /**
     * Check whether string is null or empty.
     *
     * @param str Input string.
     * @return Boolean. True means input is null or empty. False means input is a valid string.
     */
    public static boolean isStringEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Get server credential from Maven settings by server Id.
     *
     * @param settings Maven settings object.
     * @param serverId Server Id.
     * @return Server object if it exists in settings. Otherwise return null.
     */
    public static Server getServer(final Settings settings, final String serverId) {
        if (settings == null || isStringEmpty(serverId)) {
            return null;
        }
        return settings.getServer(serverId);
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param server Server object.
     * @param key    Key string.
     * @return String value if key exists; otherwise, return null.
     */
    public static String getValueFromServerConfiguration(final Server server, final String key) {
        if (server == null) {
            return null;
        }

        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        final Xpp3Dom node = configuration.getChild(key);
        if (node == null) {
            return null;
        }

        return node.getValue();
    }

    /**
     *  Get string value from plugin descriptor file, namely /META-INF/maven/pugin.xml .
     *
     * @param tagName
     *      Valid tagName in /META-INF/maven/plugin.xml, such as "artifactId", "version" etc..
     * @return
     */
    public static String getValueFromPluginDescriptor(final String tagName) {
        try (final InputStream is = Utils.class.getResourceAsStream("/META-INF/maven/plugin.xml")) {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            return doc.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (Exception e) {
        }
        return null;
    }
}
