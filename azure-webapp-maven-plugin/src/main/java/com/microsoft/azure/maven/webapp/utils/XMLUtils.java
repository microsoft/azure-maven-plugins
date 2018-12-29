/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.dom.DOMElement;
import org.dom4j.tree.AbstractElement;

import java.util.List;

public class XMLUtils {
    public static Element combineXMLNode(Element previous, Element newNode) {
        // combine attributes
        previous.setText(newNode.getText());
        for (final Attribute attribute : newNode.attributes()) {
            previous.addAttribute(attribute.getName(), attribute.getValue());
        }
        // combine node
        for (final Element child : newNode.elements()) {
            final Element previousChild = previous.element(child.getName());
            if (previousChild == null) {
                previous.add((Element) child.clone());
            } else {
                combineXMLNode(previousChild, child);
            }
        }
        return previous;
    }

    public static void setNamespace(Element element, Namespace nameSpace) {
        if (element instanceof AbstractElement) {
            ((AbstractElement) element).setNamespace(nameSpace);
        }
        for (final Element child : element.elements()) {
            setNamespace(child, nameSpace);
        }
    }

    public static String getChildValue(String attribute, Element element) {
        final Element child = element.element(attribute);
        return child == null ? null : child.getText();
    }

    public static Element getOrCreateSubElement(String name, Element element) {
        Element result = element.element(name);
        if (result == null) {
            result = new DOMElement(name);
            element.add(result);
        }
        return result;
    }

    public static DOMElement createListElement(String name, String subName,
                                               List<String> values) {
        final DOMElement resultNode = new DOMElement(name);
        for (final String value : values) {
            resultNode.add(createSimpleElement(subName, value));
        }
        return resultNode;
    }

    public static DOMElement createSimpleElement(String name, String value) {
        final DOMElement result = new DOMElement(name);
        result.setText(value);
        return result;
    }
}
