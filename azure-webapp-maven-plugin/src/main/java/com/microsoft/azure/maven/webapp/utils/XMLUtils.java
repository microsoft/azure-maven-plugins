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

    public static String getChildValue(String attribute, Element element) {
        final Element child = element.element(attribute);
        return child == null ? null : child.getText();
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
                                              List<String> value) {
        if (value != null && value.size() > 0) {
            element.add(createListElement(attribute, subAttribute, value));
        }
    }
}
