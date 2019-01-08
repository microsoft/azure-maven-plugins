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

public class WebAppPomHandler {

    public static final String PLUGIN_GROUPID = "com.microsoft.azure";
    public static final String PLUGIN_ARTIFACTID = "azure-webapp-maven-plugin";
    public static final String PLUGIN_XPATH = "/build/plugins/plugin[groupId=%s,artifactId=%s]";

    final File file;
    final Document document;

    public WebAppPomHandler(String fileName) throws DocumentException {
        final SAXReader reader = new SAXReader();
        this.file = new File(fileName);
        this.document = reader.read(this.file);
    }

    public boolean hasConfiguration() {
        final Element mavenPlugin = getMavenPluginElement();
        return mavenPlugin != null && mavenPlugin.element("configuration") != null;
    }

    public void convertToV2Configuration(WebAppConfiguration configurations) throws IOException, MojoFailureException {
        final Element pluginElement = getMavenPluginElement();
        final Element configurationNode = XMLUtils.getOrCreateSubElement("configuration", pluginElement);
        removeV1ConfigurationNodes(configurationNode);
        savePluginConfiguration(configurations);
    }

    private void removeV1ConfigurationNodes(Element configuration) {
        XMLUtils.removeNode(configuration, "javaVersion");
        XMLUtils.removeNode(configuration, "webContainer");
        XMLUtils.removeNode(configuration, "linuxRuntime");
        XMLUtils.removeNode(configuration, "containerSettings");
        XMLUtils.removeNode(configuration, "deploymentType");
        XMLUtils.removeNode(configuration, "warFile");
        XMLUtils.removeNode(configuration, "jarFile");
        XMLUtils.removeNode(configuration, "resources");
    }

    public void savePluginConfiguration(WebAppConfiguration configurations) throws IOException,
        MojoFailureException {
        final ConfigurationSerializer serializer = new V2ConfigurationSerializer();

        Element pluginElement = getMavenPluginElement();
        if (pluginElement == null) {
            // create webapp node in pom
            final Element buildNode = XMLUtils.getOrCreateSubElement("build", document.getRootElement());
            final Element pluginsRootNode = XMLUtils.getOrCreateSubElement("plugins", buildNode);
            pluginElement = createNewMavenPluginNode();
            pluginsRootNode.add(pluginElement);
        }
        final Element configuration = XMLUtils.getOrCreateSubElement("configuration", pluginElement);
        serializer.saveToXML(configurations, configuration);
        XMLUtils.setNamespace(pluginElement, document.getRootElement().getNamespace());
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
                if (PLUGIN_GROUPID.equals(groupId) && PLUGIN_ARTIFACTID.equals(artifactId)) {
                    return element;
                }
            }
        } catch (NullPointerException e) {
            return null;
        }
        return null;
    }

    private static Element createNewMavenPluginNode() {
        final Element result = new DOMElement("plugin");
        result.add(XMLUtils.createSimpleElement("groupId", PLUGIN_GROUPID));
        result.add(XMLUtils.createSimpleElement("artifactId", PLUGIN_ARTIFACTID));
        return result;
    }
}
