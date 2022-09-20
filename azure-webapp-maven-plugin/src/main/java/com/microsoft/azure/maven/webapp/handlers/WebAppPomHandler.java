/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.utils.PomUtils;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.serializer.ConfigurationSerializer;
import com.microsoft.azure.maven.webapp.serializer.V2ConfigurationSerializer;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;

// todo: migrate to com.microsoft.azure.maven.utils.PomUtils
public class WebAppPomHandler {

    public static final String PLUGIN_GROUP_ID = "com.microsoft.azure";
    public static final String PLUGIN_ARTIFACT_ID = "azure-webapp-maven-plugin";

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

    public void updatePluginConfiguration(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs, MavenProject project, PluginDescriptor plugin)
        throws IOException, MojoFailureException, DocumentException {
        final File pom = project.getFile();
        final Element pluginNode = PomUtils.getPluginNode(plugin, pom);
        Element configNode = PomUtils.getOrCreateNode(pluginNode, "configuration");
        // newly created nodes are not LocationAwareElement
        while (!(configNode.getParent() instanceof PomUtils.LocationAwareElement)) {
            configNode = configNode.getParent();
        }
        final ConfigurationSerializer serializer = new V2ConfigurationSerializer(newConfigs, oldConfigs);
        serializer.saveToXML(configNode);
        XMLUtils.setNamespace(pluginNode, pluginNode.getNamespace());
        FileUtils.fileWrite(pom, PomUtils.formatNode(FileUtils.fileRead(pom), (PomUtils.LocationAwareElement) configNode.getParent(), configNode));
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
}
