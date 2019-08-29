/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.pom;

import com.microsoft.azure.maven.spring.configuration.AppSettings;
import com.microsoft.azure.maven.spring.configuration.DeploymentSettings;
import com.microsoft.azure.maven.spring.utils.IndentUtil;
import com.microsoft.azure.maven.spring.utils.XmlUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PomXmlUpdater {
    private MavenProject project;
    private PluginDescriptor plugin;

    public PomXmlUpdater(MavenProject project, PluginDescriptor plugin) {
        this.project = project;
        this.plugin = plugin;
    }

    public void updateSettings(AppSettings app, DeploymentSettings deploy) throws DocumentException, IOException {
        final File pom = this.project.getFile();
        final SAXReader reader = new CustomSAXReader();
        reader.setDocumentFactory(new LocatorAwareDocumentFactory());
        final Document doc = reader.read(new InputStreamReader(new FileInputStream(pom)));
        final List pathsToBuild = Arrays.asList("build", "plugins");
        final Element pluginsNode = createToPath(doc.getRootElement(), pathsToBuild);
        Element springPluginNode = null;
        for (final Element element : pluginsNode.elements()) {
            final String groupId = XmlUtils.getChildValue("groupId", element);
            final String artifactId = XmlUtils.getChildValue("artifactId", element);
            final String version = XmlUtils.getChildValue("version", element);
            if (plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId) &&
                    (plugin.getVersion().equals(version) || version == null)) {
                springPluginNode = element;
            }
        }
        if (springPluginNode == null) {
            springPluginNode = createMavenSpringPluginNode(pluginsNode);
        }

        final Element configurationNode = createToPath(springPluginNode, Arrays.asList("configuration"));
        app.applyToDom4j(configurationNode);
        final Element deployNode = createToPath(configurationNode, Arrays.asList("deployment"));
        deploy.applyToDom4j(deployNode);
        // newly created nodes are not LocationAwareElement
        Element firstCreatedNode = configurationNode;
        while (!(firstCreatedNode.getParent() instanceof LocationAwareElement)) {
            firstCreatedNode = firstCreatedNode.getParent();
        }
        FileUtils.fileWrite(pom, formatElement(firstCreatedNode.getParent(), firstCreatedNode));
    }

    private static String formatElement(Element parent, Element self) {
        final String[] ls = IndentUtil.splitLines(parent.getDocument().asXML());
        final String baseIndent = IndentUtil.calcXmlIndent(ls, ((LocationAwareElement) parent).getLineNumber() - 1,
                ((LocationAwareElement) parent).getColumnNumber() - 2);
        final String placeHolder = String.format("@PLACEHOLDER_RANDOM_%s@", RandomUtils.nextLong());
        final Text placeHolderNode = new DefaultText("\n" + placeHolder);
        // replace target node to placeholder
        parent.content().replaceAll(t -> t == self ? placeHolderNode : t);
        self.setParent(null);
        // remove all spaces before target node
        XmlUtils.trimTextBeforeEnd(parent, placeHolderNode);
        final String originalXml = parent.getDocument().asXML();

        final String[] ts = IndentUtil.splitLines(XmlUtils.prettyPrintElementNoNamespace(self));
        final String replacement = Arrays.stream(ts).map(t -> baseIndent + "    " + t).collect(Collectors.joining("\n")) + "\n" + baseIndent;
        return originalXml.replace(placeHolder, replacement);
    }

    private Element createMavenSpringPluginNode(Element pluginsRootNode) throws IOException {
        final Element result = new DOMElement("plugin");
        XmlUtils.addDomWithKeyValue(result, "groupId", this.plugin.getGroupId());
        XmlUtils.addDomWithKeyValue(result, "artifactId", this.plugin.getArtifactId());
        XmlUtils.addDomWithKeyValue(result, "version", this.plugin.getVersion());
        pluginsRootNode.add(result);
        return result;
    }

    private static Element createToPath(Element node, Iterable<String> paths) {
        for (final String path : paths) {
            Element newNode = node.element(path);
            if (newNode == null) {
                newNode = new DOMElement(path);
                node.add(newNode);
            }
            node = newNode;
        }
        return node;
    }

    // code copied from https://stackoverflow.com/questions/36006819/use-dom4j-to-locate-the-node-with-line-number
    static class CustomSAXReader extends SAXReader {
        @Override
        protected SAXContentHandler createContentHandler(XMLReader reader) {
            return new CustomSAXContentHandler(getDocumentFactory(), getDispatchHandler());
        }

        @Override
        public void setDocumentFactory(DocumentFactory documentFactory) {
            super.setDocumentFactory(documentFactory);
        }

    }

    static class CustomSAXContentHandler extends SAXContentHandler {

        private Locator locator;

        // this is already in SAXContentHandler, but private
        private DocumentFactory documentFactory;

        public CustomSAXContentHandler(DocumentFactory documentFactory, ElementHandler elementHandler) {
            super(documentFactory, elementHandler);
            this.documentFactory = documentFactory;
        }

        @Override
        public void setDocumentLocator(Locator documentLocator) {
            super.setDocumentLocator(documentLocator);
            this.locator = documentLocator;
            if (documentFactory instanceof LocatorAwareDocumentFactory) {
                ((LocatorAwareDocumentFactory) documentFactory).setLocator(documentLocator);
            }

        }

        public Locator getLocator() {
            return locator;
        }
    }

    static class LocatorAwareDocumentFactory extends DocumentFactory {
        private static final long serialVersionUID = 7388661832037675334L;
        private Locator locator;

        public LocatorAwareDocumentFactory() {
            super();
        }

        public void setLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public Element createElement(QName qname) {
            final LocationAwareElement element = new LocationAwareElement(qname);
            if (locator != null) {
                element.setLineNumber(locator.getLineNumber());
                element.setColumnNumber(locator.getColumnNumber());
            }
            return element;
        }

    }

    /**
     * An Element that is aware of it location (line number in) in the source document
     */
    static class LocationAwareElement extends DefaultElement {
        private static final long serialVersionUID = 260126644771458700L;
        private int lineNumber;
        private int columnNumber;

        public LocationAwareElement(QName qname) {
            super(qname);
        }

        public LocationAwareElement(QName qname, int attributeCount) {
            super(qname, attributeCount);

        }

        public LocationAwareElement(String name, Namespace namespace) {
            super(name, namespace);

        }

        public LocationAwareElement(String name) {
            super(name);

        }

        /**
         * @return the lineNumber
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * @param lineNumber the lineNumber to set
         */
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        /**
         * @return the columnNumber
         */
        public int getColumnNumber() {
            return columnNumber;
        }

        /**
         * @param columnNumber the columnNumber to set
         */
        public void setColumnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
        }

    }
}
