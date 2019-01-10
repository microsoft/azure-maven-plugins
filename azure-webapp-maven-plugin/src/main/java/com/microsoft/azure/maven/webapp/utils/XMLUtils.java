/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.dom.DOMElement;
import org.dom4j.tree.AbstractElement;

import java.util.ArrayList;
import java.util.List;

public class XMLUtils {

    public static void setNamespace(Element element, Namespace nameSpace) {
        if (element instanceof AbstractElement) {
            ((AbstractElement) element).setNamespace(nameSpace);
        }
        for (final Element child : element.elements()) {
            setNamespace(child, nameSpace);
        }
    }

    public static Element getChild(Element parent, String... paths) {
        Element result = parent;
        for (final String childName : paths) {
            if (result != null) {
                result = result.element(childName);
            } else {
                return null;
            }
        }
        return result;
    }

    public static List<String> getListValue(Element domNode) {
        if (domNode == null) {
            return null;
        }
        final List<String> result = new ArrayList<>();
        for (final Element child : domNode.elements()) {
            result.add(child.getText());
        }
        return result;
    }

    public static String getChildValue(String attribute, Element element) {
        final Element child = element.element(attribute);
        return child == null ? null : child.getText();
    }

    public static void setChildValue(String attribute, String value, Element element) {
        final Element child = element.element(attribute);
        if (child == null) {
            element.add(createSimpleElement(attribute, value));
        } else {
            element.element(attribute).setText(value);
        }
    }

    public static Element getOrCreateSubElement(String name, Element element) {
        Element result = element.element(name);
        if (result == null) {
            result = new DOMElement(name);
            ((DOMElement) result).setNamespace(element.getNamespace());
            element.add(result);
        }
        return result;
    }

    public static DOMElement createSimpleElement(String name, String value) {
        final DOMElement result = new DOMElement(name);
        result.setText(value);
        return result;
    }

    public static void removeNode(Element element, String attribute) {
        final Element aim = element.element(attribute);
        if (aim != null) {
            element.remove(aim);
        }
    }

    public static void addNotEmptyElement(Element element, String attribute, String value) {
        if (StringUtils.isNotEmpty(value)) {
            element.add(createSimpleElement(attribute, value));
        }
    }

    public static void addNotEmptyListElement(Element element, String attribute, String subAttribute,
                                              List<String> values) {
        if (values != null && values.size() > 0) {
            final DOMElement resultNode = new DOMElement(attribute);
            for (final String value : values) {
                resultNode.add(createSimpleElement(subAttribute, value));
            }
            element.add(resultNode);
        }
    }
}
