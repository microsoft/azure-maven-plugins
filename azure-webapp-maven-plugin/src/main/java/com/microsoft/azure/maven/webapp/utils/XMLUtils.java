/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.List;

public class XMLUtils {

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
}
