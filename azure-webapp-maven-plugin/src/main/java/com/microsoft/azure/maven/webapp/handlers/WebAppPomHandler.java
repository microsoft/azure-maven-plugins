/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.serializer.ConfigurationSerializer;
import com.microsoft.azure.maven.webapp.serializer.V2ConfigurationSerializer;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WebAppPomHandler {

    public static final String PLUGIN_GROUP_ID = "com.microsoft.azure";
    public static final String PLUGIN_ARTIFACT_ID = "azure-webapp-maven-plugin";
    public static final String PLUGIN_PROPERTIES_FILE = "plugin.properties";
    public static final String PROPERTY_VERSION = "project.version";

    final File file;
    final Document document;

    public WebAppPomHandler(String fileName) throws DocumentException {
        final SAXReader reader = new SAXReader();
        this.file = new File(fileName);
        this.document = reader.read(this.file);
    }

    public Element getConfiguration() {
        final Element mavenPlugin = getMavenPluginElement();
        return mavenPlugin == null ? null : mavenPlugin.element("configuration");
    }

    public void updatePluginConfiguration(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs)
        throws IOException,
        MojoFailureException {
        Element pluginElement = getMavenPluginElement();
        if (pluginElement == null) {
            final Element buildNode = XMLUtils.getOrCreateSubElement("build", document.getRootElement());
            final Element pluginsRootNode = XMLUtils.getOrCreateSubElement("plugins", buildNode);
            pluginElement = createNewMavenPluginNode(pluginsRootNode);
        }
        final Element configuration = XMLUtils.getOrCreateSubElement("configuration", pluginElement);
        final ConfigurationSerializer serializer = new V2ConfigurationSerializer(newConfigs, oldConfigs);
        serializer.saveToXML(configuration);
        XMLUtils.setNamespace(pluginElement, pluginElement.getNamespace());
        saveModel();
    }

    private void saveModel() throws IOException {
        final XMLWriter writer = new XMLWriter(new FileWriter(file), OutputFormat.createPrettyPrint());
        writer.setEscapeText(false);
        writer.write(document);
        writer.close();
    }

    // get webapp maven plugin node from pom
    private Element getMavenPluginElement() {
        try {
            final Element pluginsRoot = document.getRootElement().element("build").element("plugins");
            for (final Element element : pluginsRoot.elements()) {
                final String groupId = XMLUtils.getChildValue("groupId", element);
                final String artifactId = XMLUtils.getChildValue("artifactId", element);
                if (PLUGIN_GROUP_ID.equals(groupId) && PLUGIN_ARTIFACT_ID.equals(artifactId)) {
                    return element;
                }
            }
        } catch (NullPointerException e) {
            return null;
        }
        return null;
    }

    private static Element createNewMavenPluginNode(Element pluginsRootNode) throws IOException {

        final Element result = new DOMElement("plugin");

        ((DOMElement) result).setNamespace(pluginsRootNode.getNamespace());
        result.add(XMLUtils.createSimpleElement("groupId", PLUGIN_GROUP_ID));
        result.add(XMLUtils.createSimpleElement("artifactId", PLUGIN_ARTIFACT_ID));
        result.add(XMLUtils.createSimpleElement("version", getPluginVersion()));
        pluginsRootNode.add(result);
        return result;
    }

    private static String getPluginVersion() throws IOException {
        final Properties properties = new Properties();
        try (final InputStream is = WebAppPomHandler.class.getClassLoader()
                .getResourceAsStream(PLUGIN_PROPERTIES_FILE)) {
            properties.load(is);
            return properties.getProperty(PROPERTY_VERSION);
        }
    }
}
