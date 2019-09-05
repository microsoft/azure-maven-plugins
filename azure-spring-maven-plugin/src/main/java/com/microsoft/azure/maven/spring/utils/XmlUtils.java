/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.AbstractElement;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class XmlUtils {
    public static String getChildValue(String attribute, Element element) {
        final Element child = element.element(attribute);
        return child == null ? null : child.getText();
    }

    public static String prettyPrintElementNoNamespace(Element node) {
        removeAllNamespaces(node);
        try {
            final StringWriter out = new StringWriter();
            final OutputFormat format = OutputFormat.createPrettyPrint();
            format.setSuppressDeclaration(true);
            format.setIndent("    "); // 4 spaces
            format.setPadText(false);
            final XMLWriter writer = new XMLWriter(out, format);
            writer.write(node);
            writer.flush();

            return StringUtils.stripStart(out.toString(), null);
        } catch (IOException e) {
            throw new RuntimeException("IOException while generating " + "textual representation: " + e.getMessage());
        }

    }

    public static void trimTextBeforeEnd(Element parent, Node target) {
        final List<Node> children = parent.content();
        final int index = children.indexOf(target);
        int pos = index - 1;
        while (pos >= 0 && children.get(pos).getNodeType() == Node.TEXT_NODE) {
            final Node textNode = children.get(pos--);
            textNode.setText(StringUtils.stripEnd(textNode.getText(), null));
            if (StringUtils.isNotBlank(textNode.getText())) {
                break;
            }
        }

        final int size = children.size();
        pos = index + 1;
        while (pos < size && children.get(pos).getNodeType() == Node.TEXT_NODE) {
            final Node textNode = children.get(pos++);
            textNode.setText(StringUtils.stripStart(textNode.getText(), null));
            if (StringUtils.isNotBlank(textNode.getText())) {
                break;
            }
        }
    }

    private static void setNamespace(Element element, Namespace nameSpace) {
        if (element instanceof AbstractElement) {
            ((AbstractElement) element).setNamespace(nameSpace);
        }
        for (final Element child : element.elements()) {
            setNamespace(child, nameSpace);
        }
    }

    public static void addDomWithValueList(Element element, String attribute, String subAttribute, List<String> values) {
        if (values != null && !values.isEmpty()) {
            final DOMElement resultNode = new DOMElement(attribute);
            for (final String value : values) {
                resultNode.add(createSimpleElement(subAttribute, value));
            }
            element.add(resultNode);
        }
    }

    public static void addDomWithKeyValue(Element node, String key, Object value) {
        final DOMElement newNode = new DOMElement(key);
        if (value != null) {
            newNode.setText(value.toString());
        }
        node.add(newNode);
    }

    // code copied from https://stackoverflow.com/questions/1422395/clean-namespace-handling-with-dom4j
    public static void removeAllNamespaces(Element ele) {
        setNamespace(ele, Namespace.NO_NAMESPACE);
        removeNamespaces(ele.content());
    }

    /**
     * Recursively sets the namespace of the List and all children if the current namespace is match
     */
    private static void setNamespaces(List l, Namespace ns) {
        Node n = null;
        for (int i = 0; i < l.size(); i++) {
            n = (Node) l.get(i);

            if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                ((Attribute) n).setNamespace(ns);
            }
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                setNamespaces((Element) n, ns);
            }
        }
    }

    /**
     * Recursively removes the namespace of the list and all its children: sets to Namespace.NO_NAMESPACE
     */
    private static void removeNamespaces(List l) {
        setNamespaces(l, Namespace.NO_NAMESPACE);
    }

    /**
     * Recursively sets the namespace of the element and all its children.
     */
    private static void setNamespaces(Element elem, Namespace ns) {
        setNamespace(elem, ns);
        setNamespaces(elem.content(), ns);
    }

    private static DOMElement createSimpleElement(String name, String value) {
        final DOMElement result = new DOMElement(name);
        result.setText(value);
        return result;
    }

    private XmlUtils() {

    }
}
